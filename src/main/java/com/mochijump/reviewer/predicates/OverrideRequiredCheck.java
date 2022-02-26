package com.mochijump.reviewer.predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@AllArgsConstructor
@Component
@Slf4j
public class OverrideRequiredCheck implements Predicate<PredicateDTO> {
    private GlobalValues values;

    private ObjectMapper mapper;

    @Override
    public boolean test(PredicateDTO dto) {
        if (values.getMethodsThatCauseMissingInfo().contains(dto.getMethod())) {
            throw new IllegalStateException(
                    String.format(
                            "Sorry, this is super annoying and dumb, but the return information is completely absent "
                                    + "from XXXScan AFAIK. Look up the tx hash and manuallyOverride as a buy dto=%s",
                            mapper.valueToTree(dto)));
        }
        return false;
    }
}
