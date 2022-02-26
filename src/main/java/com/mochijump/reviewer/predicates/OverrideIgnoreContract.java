package com.mochijump.reviewer.predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@AllArgsConstructor
@Component
@Slf4j
public class OverrideIgnoreContract extends IsOverride {
    public GlobalValues globalValues;
    public StateMaintainer stateMaintainer;
    public ObjectMapper mapper;

    @Override
    public boolean test(final PredicateDTO dto) {
        /**
         * This method is for ignoring interactions with wrappers. Will collect the gas lost though
         */
        if (globalValues.getContractIgnoreCompletely().contains(dto.getTo())) {
            stateMaintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                            dto.getTxCostAtTheTime() / dto
                                                    .getNativeCoinHistoricalDollarPerUnit());
            return true;
        }
        return false;
    }
}