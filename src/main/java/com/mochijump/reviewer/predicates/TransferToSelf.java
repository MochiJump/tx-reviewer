package com.mochijump.reviewer.predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@AllArgsConstructor
@Component
@Slf4j
public class TransferToSelf implements Predicate<PredicateDTO> {
    public StateMaintainer stateMaintainer;
    public ObjectMapper mapper;

    @Override
    public boolean test(PredicateDTO dto) {
        final String ownerWallet = stateMaintainer.getOwnershipState().getWallet();
        //TODO should have override to report sale
        if (
                StringUtils.equals(dto.getFrom(), ownerWallet) &&
                        !StringUtils.equals(dto.getTo(), ownerWallet) &&
                        dto.getErc20Interactions().isEmpty() &&
                        dto.getNativeCoinInAmount() == 0 &&
                        dto.getNativeCoinOutAmount() > 0
        ) {
            log.info("Determined this is a transfer and is still owned by user dto={}",
                     mapper.valueToTree(dto));
            //lost as gas
            stateMaintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                            dto.getTxCostAtTheTime() / dto.getNativeCoinHistoricalDollarPerUnit());
            return true;
        }
        return false;
    }
}
