package com.mochijump.reviewer.predicates;

import com.mochijump.reviewer.interfaces.StateMaintainer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@AllArgsConstructor
@Component
@Slf4j
public class ApprovePredicate implements Predicate<PredicateDTO> {
    private StateMaintainer maintainer;

    @Override
    public boolean test(PredicateDTO dto) {
        //TODO make this list that is checked against configurable
        if (StringUtils.equals(dto.getMethod(), "Approve") || (StringUtils.equals(dto.getMethod(), "Approve Delegation"))) {
            maintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                       dto.getTxCostAtTheTime() / dto.getNativeCoinHistoricalDollarPerUnit());
            return true;
        }
        return false;
    }

}
