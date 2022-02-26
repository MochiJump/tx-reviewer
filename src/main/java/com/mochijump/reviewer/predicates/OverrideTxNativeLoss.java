package com.mochijump.reviewer.predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

@AllArgsConstructor
@Component
@Slf4j
public class OverrideTxNativeLoss extends IsOverride {
    private GlobalValues globalValues;
    private StateMaintainer maintainer;
    private ObjectMapper mapper;

    @Override
    public boolean test(final PredicateDTO dto) {
        Optional<Double> result =
                Optional.ofNullable(globalValues.getTxNativeLossOverride().get(dto.getTxHash()));
        if (result.isPresent()) {
            Double realized = result.get();
            log.info("TxNativeLossOverride returning true realized={} dto={}", mapper.valueToTree(realized),
                     mapper.valueToTree(dto));
            maintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(), realized);
            return true;
        }
        return false;
    }
}
