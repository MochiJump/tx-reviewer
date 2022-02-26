package com.mochijump.reviewer.predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.values.GlobalValues;
import com.mochijump.reviewer.interfaces.HistoricalPriceLookUp;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@AllArgsConstructor
@Component
@Slf4j
public class HarvestRewards implements Predicate<PredicateDTO> {
    public GlobalValues globalValues;
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
                        dto.getNativeCoinOutAmount() == 0 &&
                        !dto.getErc20Interactions().isEmpty() &&
                        dto.getErc20Interactions().size() >= 1 &&
                        globalValues.getHarvestRewardsMethodNames() != null &&
                        globalValues.getHarvestRewardsMethodNames().contains(dto.getMethod())
        ) {
            for (final PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                if (!StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                        StringUtils.equals(interaction.getTo(), ownerWallet)) {
                    stateMaintainer.addZeroCostBasis(interaction.getCoinSymbol(), dto.getTimeOfTx(),
                                                     interaction.getErc20Amount());
                }
                if (StringUtils.equals(interaction.getFrom(), ownerWallet) ||
                        !StringUtils.equals(interaction.getTo(), ownerWallet)) {
                    // should never happen for this condition
                    return false;
                }
            }
            //gas
            stateMaintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                            dto.getTxCostAtTheTime() / dto
                                                    .getNativeCoinHistoricalDollarPerUnit());
            return true;
        }
        return false;
    }
}
