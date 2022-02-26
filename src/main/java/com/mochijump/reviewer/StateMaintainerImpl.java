package com.mochijump.reviewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.HistoricalPriceLookUp;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.models.OwnershipState;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mochijump.reviewer.models.OwnershipState.Verb.BUY;
import static com.mochijump.reviewer.models.OwnershipState.Verb.LOSE;
import static com.mochijump.reviewer.models.OwnershipState.Verb.SELL;

/**
 * Note, this component carries state. This means that we need to avoid concurrency issues with writes to this
 * object's state, and this class must remain a singleton (default value of @Component)
 */
@Slf4j
public class StateMaintainerImpl implements StateMaintainer {
    private final OwnershipState ownershipState = new OwnershipState();
    private final ObjectMapper mapper;
    private final HistoricalPriceLookUp historicalPriceLookUp;


    public StateMaintainerImpl(
            final ObjectMapper objectMapper, final HistoricalPriceLookUp historicalPriceLookUp, final
    GlobalValues globalValues) {
        this.mapper = objectMapper;
        this.historicalPriceLookUp = historicalPriceLookUp;
        this.ownershipState.setWallet(globalValues.getWallet());
    }

    @Override
    public void addValueToFluxState(final String name, final Instant instant, final Double value, final Double amount) {
        OwnershipState.Entry entry = new OwnershipState.Entry(instant, amount, value);
        if (ownershipState.getInFlux().get(name) != null) {
            ownershipState.getInFlux().get(name).add(entry);
        } else {
            //TODO need consider either smartly inserting here or something to keep everything organized by Instant
            final OwnershipState.EntryTreeSet<OwnershipState.Entry> set = new OwnershipState.EntryTreeSet<>();
            set.add(entry);
            ownershipState.getInFlux().put(name, set);
        }
        log.info("BUYING  {}, result={}", name, mapper.valueToTree(entry));
    }

    @Override
    public void addValueToMaster(String name, Instant instant, Double value, Double amount) {
        addValueToMaster(name, instant, value, amount, Optional.empty());
    }

    @Override
    public void addValueToMaster(String name, Instant instant, Double value, Double amount, Optional<Boolean> isLp) {
        addOrRemoveFromMaster(instant, name, amount, value, BUY, isLp);
        log.info("Recording buy {}", name);
    }

    @Override
    public void addLossToMaster(String name, Instant instant, Double amount) {
        addLossToMaster(name, instant, amount, Optional.empty());
    }

    @Override
    public void addLossToMaster(String name, Instant instant, Double amount, Optional<Boolean> isLp) {
        addOrRemoveFromMaster(instant, name, amount, 0D, LOSE, isLp);
        log.info("Recording loss {}", name);
    }

    @Override public void cleanupState() {
        //step 1 clean up lower level
        ownershipState.getInFlux().entrySet().forEach(map -> {
            map.getValue().removeIf(entry -> entry.getAmount() == 0D);
        });
        //step 2 clean up top level
        final Map<String, OwnershipState.EntryTreeSet<OwnershipState.Entry>> cleanedUpState =
                ownershipState.getInFlux().entrySet().stream()
                              .filter(es -> es.getValue() != null && !es.getValue().isEmpty())
                              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        ownershipState.setInFlux(cleanedUpState);
        //step 3 replace merge wrapped and unwrapped tokens
        final Set<String> keys =
                ownershipState.getMasterSet().stream().map(ms -> ms.getCoinName()).collect(Collectors.toSet());
        for (String key : keys) {
            final String snippedKey = key.substring(1);
            if (keys.contains(snippedKey)) {
                final OwnershipState.EntryTreeSet<OwnershipState.EntryWithVerb>
                        entrySet = new OwnershipState.EntryTreeSet<>();
                entrySet.addAll(
                        ownershipState.getMasterSet().stream()
                                      .filter(ms -> StringUtils.equals(ms.getCoinName(), key))
                                      .map(ms -> {
                                          ms.setCoinName(snippedKey);
                                          return ms;
                                      })
                                      .collect(Collectors.toSet()));
                ownershipState.getMasterSet().removeIf(ms -> StringUtils.equals(ms.getCoinName(), key));
                ownershipState.getMasterSet().addAll(entrySet);
            }
        }
    }

    @Override
    public OwnershipState getOwnershipState() {
        return this.ownershipState;
    }

    @Override public void loadStateFromFile(Optional<String> path) {
        if (path.isPresent()) {
            throw new IllegalStateException("not implemented yet");
        }
        log.info("Not loading previously existing flux state. Starting from scratch");
    }

    @Override
    public void removeValueFromFluxState(
            final String name, final Double amountSold, final Double valueOfSale, final Instant timeOfSale) {
        Double amountLeftToSell = amountSold;
        final OwnershipState.EntryTreeSet<OwnershipState.Entry> sets = ownershipState.getInFlux().get(name);

        for (var set : sets) {
            if (amountLeftToSell.equals(0)) {
                break;
            }
            // 1. did I use the entire block?
            if (amountLeftToSell >= set.getAmount()) {
                amountLeftToSell = amountLeftToSell - set.getAmount();
                // determine avg cost of new and old
                final Double oldValue = set.getValue();
                final Double newValue = valueOfSale / amountSold * set.getAmount();
                determineGainOrLoss(newValue, oldValue, ownershipState, set.getTime(), timeOfSale, name, amountSold);
                set.setAmount(0D);
                set.setValue(0D);

            } else {
                final Double amountLeft = set.getAmount() - amountLeftToSell;
                final Double oldValuePerUnit = set.getValue() / set.getAmount();
                final Double oldValueOfAmountSold = oldValuePerUnit * amountLeftToSell;
                final Double newValueOfAmountSold = valueOfSale / amountSold * amountLeftToSell;
                determineGainOrLoss(newValueOfAmountSold, oldValueOfAmountSold, ownershipState, set.getTime(),
                                    timeOfSale, name, amountSold);
                set.setValue(oldValuePerUnit * amountLeft);
                set.setAmount(amountLeft);
                break;
            }
        }
        cleanupState();
        log.info(
                "SELLING  {} {}, short term gains {}, long term "
                        + "gains {}, resulting state={} ",
                name, amountSold, ownershipState.getShortTermGains(), ownershipState.getLongTermGains(),
                mapper.valueToTree(ownershipState.getInFlux().get(name)));
    }

    @Override public void removeValueFromMaster(String name, final Instant instant, Double amountSold, Double value) {
        log.info("Recording sell {}", name);
        addOrRemoveFromMaster(instant, name, amountSold, value, SELL);
    }

    @Override public void saveCurrentState(OwnershipState state) {
        throw new IllegalStateException("not implemented yet");
    }

    public void finalizeMasterToInFlux() {
        cleanupState();
        //TODO clean up after testing
        final List<OwnershipState.EntryWithVerb> whatHasBeenTouched = new ArrayList<>();
        this.ownershipState.getMasterSet().stream().forEach(entry -> {
            whatHasBeenTouched.add(entry);
            final String name = entry.getCoinName();
            if (entry.getValue() == 0D && !entry.getVerb().equals(LOSE)) {
                log.warn("No value present when processing entry={}", mapper.valueToTree(entry));
            }
            if (entry.getVerb().equals(BUY)) {
                addValueToFluxState(name, entry.getTime(), entry.getValue(), entry.getAmount());
            } else if (entry.getVerb().equals(SELL)) {
                //TODO annoying doesn't follow the same pattern
                try {
                    removeValueFromFluxState(name, entry.getAmount(), entry.getValue(), entry.getTime());
                } catch (NullPointerException npe) {
                    log.error("Ran out of blocks to sell on entry={}", mapper.valueToTree(entry));
                    var list2 = whatHasBeenTouched.stream().filter(e -> e.getCoinName().equals(entry.getCoinName()))
                                                  .collect(Collectors.toList());
                    log.error("listOfTxForThisCoinSoFar={}", mapper.valueToTree(list2));
                    double amountBalance = 0;
                    for (var i : list2) {
                        amountBalance = i.getAmount() * (i.getVerb().equals(BUY) ? 1 : -1);
                        log.error("Stepping through remaining balance={}", amountBalance);
                    }
                    throw new IllegalArgumentException("Ran out of blocks to sell see above error msg");
                }
            } else {
                //TODO check workings here
                try {
                    removeValueFromFluxState(name, entry.getAmount(), 0D, entry.getTime());
                } catch (NullPointerException npe) {
                    log.error("Ran out of blocks to lose on entry={}", mapper.valueToTree(entry));
                    var list2 = whatHasBeenTouched.stream().filter(e -> e.getCoinName().equals(entry.getCoinName()))
                                                  .collect(Collectors.toList());
                    log.error("listOfTxForThisCoinSoFar={}", mapper.valueToTree(list2));
                    double amountBalance = 0;
                    for (var i : list2) {
                        amountBalance = amountBalance + i.getAmount() * (i.getVerb().equals(BUY) ? 1 : -1);
                        log.error("Stepping through remaining balance={}", amountBalance);
                    }
                    throw new IllegalArgumentException("Ran out of blocks to lose see above error msg");
                }
            }
        });
//        //TODO clear? not sure we want this, better to create a file right?
//        this.ownershipState.setMasterSet(new OwnershipState.EntryTreeSet<>());
    }

    @Override
    public void addZeroCostBasis(String name, Instant instant, Double amount) {
        final Double value = historicalPriceLookUp.lookupPriceWhen(instant, name) * amount;
        ownershipState.setShortTermGains(ownershipState.getShortTermGains() + value);
        ownershipState.getShortTermIrsFormatList()
             .add(OwnershipState.IrsFormat.builder().cost(0D).proceeds(value).gain(value)
                                          .dateAcquired(instant).dateSold(instant)
                                          .description(name + " " + amount + "generated").build());
        addValueToMaster(name, instant, value, amount);
    }

    private void determineGainOrLoss(
            final Double newValue, final Double oldValue, final OwnershipState state, final Instant timeOfBuy,
            final Instant timeOfSale, final String coinName, final Double amountSold) {
        final Double gainOrLoss = newValue - oldValue;
        final boolean longTerm = timeOfBuy.isBefore(timeOfSale.minus(365L, ChronoUnit.DAYS));
        log.info("determined gain/loss={}", gainOrLoss);
        if (longTerm) {
            state.getLongTermIrsFormatList()
                 .add(OwnershipState.IrsFormat.builder().cost(oldValue).proceeds(newValue).gain(gainOrLoss)
                                              .dateAcquired(timeOfBuy).dateSold(timeOfSale)
                                              .description(coinName + " " + amountSold + " sold").build());
            state.setLongTermGains(state.getLongTermGains() + gainOrLoss);
        } else {
            state.getShortTermIrsFormatList()
                 .add(OwnershipState.IrsFormat.builder().cost(oldValue).proceeds(newValue).gain(gainOrLoss)
                                              .dateAcquired(timeOfBuy).dateSold(timeOfSale)
                                              .description(coinName + " " + amountSold+ " sold").build());
            state.setShortTermGains(state.getShortTermGains() + gainOrLoss);
        }
    }


    private void addOrRemoveFromMaster(
            final Instant instant, final String name, final Double amount, final Double value, final
    OwnershipState.Verb verb) {
        addOrRemoveFromMaster(instant, name, amount, value, verb, Optional.empty());
    }

    private void addOrRemoveFromMaster(
            final Instant instant, final String name, final Double amount, final Double value, final
    OwnershipState.Verb verb, final Optional<Boolean> isLp) {
        OwnershipState.EntryWithVerb entry = new OwnershipState.EntryWithVerb(instant, amount, value, verb, name);
        isLp.ifPresent(lp -> entry.setLpToken(lp));
        ownershipState.getMasterSet().add(entry);
    }
}