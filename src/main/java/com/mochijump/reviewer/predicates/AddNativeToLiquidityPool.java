package com.mochijump.reviewer.predicates;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.HistoricalPriceLookUp;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

@AllArgsConstructor
@Component
@Slf4j
public class AddNativeToLiquidityPool implements Predicate<PredicateDTO> {
    public StateMaintainer stateMaintainer;
    public ObjectMapper mapper;
    public HistoricalPriceLookUp priceLookUp;

    @Override
    public boolean test(final PredicateDTO dto) {
        final String ownerWallet = stateMaintainer.getOwnershipState().getWallet();
        if (
                StringUtils.equals(dto.getFrom(), ownerWallet) &&
                        !StringUtils.equals(dto.getTo(), ownerWallet) &&
                        dto.getNativeCoinInAmount() == 0 &&
                        dto.getNativeCoinOutAmount() > 0 &&
                        !dto.getErc20Interactions().isEmpty() &&
                        dto.getErc20Interactions().size() == 2
        ) {
            int amountOut = 0;
            int amountIn = 0;
            //ensure it fulfills th predicate first
            for (final PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                if (StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                        !StringUtils.equals(interaction.getTo(), ownerWallet)) {
                    amountOut++;
                }
                if (!StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                        StringUtils.equals(interaction.getTo(), ownerWallet)) {
                    amountIn++;
                }
            }
            if (amountOut == 1 && amountIn == 1) {
                log.info("Determined this is an add to an LP dto={}",
                         mapper.valueToTree(dto));
                Double totalValueOut = dto.getNativeCoinOutAmount()*dto.getNativeCoinHistoricalDollarPerUnit();
                stateMaintainer.removeValueFromMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                                      dto.getNativeCoinOutAmount(),
                                                      totalValueOut);
                String lpAddress = null;
                for (final PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                    if (StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                            !StringUtils.equals(interaction.getTo(), ownerWallet)) {
                        log.info("add gas used as loss and mark the value out as sold");
                        // we're only recording the loss due to gas paid
                        stateMaintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                                        dto.getTxCostAtTheTime() / dto
                                                                .getNativeCoinHistoricalDollarPerUnit());
                        final Double valueOutForCoin = interaction.getErc20Amount() * interaction.getHistoricalPrice();
                        totalValueOut = totalValueOut + valueOutForCoin;
                        stateMaintainer.removeValueFromMaster(interaction.getCoinSymbol(), dto.getTimeOfTx(),
                                                              interaction.getErc20Amount(),
                                                              valueOutForCoin);
                    } else {
                        lpAddress = interaction.getContractAddressOfCoin();
                    }
                }
                assert lpAddress != null;
                //really? iterating through the same list 3xs
                for (final PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                    if (!StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                            StringUtils.equals(interaction.getTo(), ownerWallet)) {
                        log.info("evaluating price of liquidity pool token as {}", totalValueOut);
                        stateMaintainer.addValueToMaster(lpAddress, dto.getTimeOfTx(), totalValueOut,
                                                         interaction.getErc20Amount(), Optional.of(Boolean.TRUE));
                    }
                }
                return true;
            }

        }
        return false;
    }

}
