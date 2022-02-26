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
public class IgnoreTxCompletely extends IsOverride{
    public GlobalValues globalValues;
    public StateMaintainer stateMaintainer;
    public ObjectMapper mapper;

    @Override
    public boolean test(final PredicateDTO dto) {
        /**
         * This method is for ignoring failed transactions that don't show on the csv file as an error. Will collect
         * the gas lost though
         */
        if (globalValues.getTxIgnoreCompletely().contains(dto.getTxHash())) {
            stateMaintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                            dto.getTxCostAtTheTime() / dto
                                                    .getNativeCoinHistoricalDollarPerUnit());
            return true;
        }
        return false;
    }
}