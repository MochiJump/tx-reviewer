package com.mochijump.reviewer.predicates;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.HistoricalPriceLookUp;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

@AllArgsConstructor
@Component
@Slf4j
public class RemoveNativeFromLiquidityPoolOverride extends IsOverride {
    public StateMaintainer stateMaintainer;
    public ObjectMapper mapper;
    public HistoricalPriceLookUp priceLookUp;
    public GlobalValues values;

    /**
     * Another predicate that requires a an override...
     *
     * @param dto
     * @return
     */
    @Override
    public boolean test(final PredicateDTO dto) {
        final String ownerWallet = stateMaintainer.getOwnershipState().getWallet();
        if (
                StringUtils.equals(dto.getFrom(), ownerWallet) &&
                        !StringUtils.equals(dto.getTo(), ownerWallet) &&
                        dto.getNativeCoinInAmount() == 0 &&
                        dto.getNativeCoinOutAmount() == 0 &&
                        !dto.getErc20Interactions().isEmpty() &&
                        dto.getErc20Interactions().size() == 2 &&
                        //TODO make this pull from a configurable list
                        StringUtils.equals(dto.getMethod(), "Remove Liquidity ETH With Permit")
        ) {
            int numOutboundTx = 0;
            int numInboundTx = 0;
            //ensure it fulfills th predicate first
            for (final PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                if (StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                        !StringUtils.equals(interaction.getTo(), ownerWallet)) {
                    numOutboundTx++;
                }
                if (!StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                        StringUtils.equals(interaction.getTo(), ownerWallet)) {
                    numInboundTx++;
                }
            }
            if (numOutboundTx == 1 && numInboundTx == 1) {
                log.info("Determined this is a removal from LP dto={}",
                         mapper.valueToTree(dto));
                Double totalValueIn = 0D;
                String lpAddress = null;
                // recording the loss due to gas paid
                stateMaintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                                dto.getTxCostAtTheTime() / dto
                                                        .getNativeCoinHistoricalDollarPerUnit());
                for (final PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                    if (!StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                            StringUtils.equals(interaction.getTo(), ownerWallet)) {
                        log.info("add gas used as loss and mark the value out as sold");
                        final Double valueInForCoin = interaction.getErc20Amount() * interaction.getHistoricalPrice();
                        totalValueIn = totalValueIn + valueInForCoin;
                        stateMaintainer.addValueToMaster(interaction.getCoinSymbol(), dto.getTimeOfTx(),
                                                         valueInForCoin, interaction.getErc20Amount());
                    } else {
                        lpAddress = interaction.getContractAddressOfCoin();
                    }
                }
                final Optional<Double> nativeAmount =
                        Optional.ofNullable(values.getTxNativeAmountFromLpExit().get(dto.getTxHash()));
                if (nativeAmount.isPresent()) {
                    final Double valueInForNative = nativeAmount.get() * dto.getNativeCoinHistoricalDollarPerUnit();
                    totalValueIn = totalValueIn + valueInForNative;
                    stateMaintainer.addValueToMaster(dto.getNativeCoin(), dto.getTimeOfTx(), valueInForNative,
                                                     nativeAmount.get());

                } else {
                    throw new IllegalStateException(
                            String.format(
                                    "txHash=%s Sorry, you have to provide an override value for the amount of "
                                            + "received native tokens, as this data appears to be missing from Xscan",
                                    dto.getTxHash()));
                }
                assert lpAddress != null;
                //really? iterating through the same list 3xs
                for (final PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                    if (StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                            !StringUtils.equals(interaction.getTo(), ownerWallet)) {
                        log.info("evaluating price paid for liquidity pool tokens as {}", totalValueIn);
                        stateMaintainer
                                .removeValueFromMaster(lpAddress, dto.getTimeOfTx(), interaction.getErc20Amount(),
                                                       totalValueIn);
                    }
                }
                return true;
            }

        }
        return false;
    }

}
