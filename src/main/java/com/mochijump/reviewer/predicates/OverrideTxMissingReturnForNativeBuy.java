package com.mochijump.reviewer.predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

@AllArgsConstructor
@Component
@Slf4j
public class OverrideTxMissingReturnForNativeBuy extends IsOverride {
    private GlobalValues globalValues;
    private StateMaintainer maintainer;
    private ObjectMapper mapper;

    @Override
    public boolean test(final PredicateDTO dto) {
        Optional<Double> result =
                Optional.ofNullable(globalValues.getTxNativeBuyReturnMissing().get(dto.getTxHash()));
        if (result.isPresent() && dto.getErc20Interactions().size() == 1) {
            Double amount = result.get();
            log.info("TxMissingReturnForNativeBuy adding buy of {} for amount of {} dto={}", dto.getNativeCoin(),
                     amount, mapper.valueToTree(dto));
            // gas
            maintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                       dto.getTxCostAtTheTime() / dto.getNativeCoinHistoricalDollarPerUnit());
            maintainer.addValueToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                        dto.getNativeCoinHistoricalDollarPerUnit() * amount, amount);
            for (PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                maintainer.removeValueFromMaster(interaction.getCoinSymbol(), dto.getTimeOfTx(),
                                                 interaction.getErc20Amount(),
                                                 interaction.getHistoricalPrice() * interaction.getErc20Amount());
            }
            return true;
        }
        return false;
    }
}
