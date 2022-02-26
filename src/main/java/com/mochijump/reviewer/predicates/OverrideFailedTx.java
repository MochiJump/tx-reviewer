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
public class OverrideFailedTx extends IsOverride {
    private StateMaintainer maintainer;

    @Override
    public boolean test(final PredicateDTO dto) {
        if (!StringUtils.isBlank(dto.getError())) {
            log.info("Transaction failed, recording gas loss");
            maintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                       dto.getTxCostAtTheTime() / dto.getNativeCoinHistoricalDollarPerUnit());
            return true;

        }
        return false;
    }

}