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
public class TxCancelationAttempt implements Predicate<PredicateDTO> {

    private StateMaintainer maintainer;

    @Override
    public boolean test(PredicateDTO dto) {
        final String ownerWallet = maintainer.getOwnershipState().getWallet();
        if (StringUtils.equals(dto.getMethod(), "Transfer") && StringUtils.equals(dto.getFrom()
                , ownerWallet) && StringUtils.equals(dto.getTo(), ownerWallet) && dto.getErc20Interactions().isEmpty()
                && dto.getNativeCoinInAmount() == 0D && dto.getNativeCoinOutAmount() == 0D){
            //lost as gas
            maintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                       dto.getTxCostAtTheTime() / dto.getNativeCoinHistoricalDollarPerUnit());
            log.info("Found transaction cancellation attempt");
            return true;
        }
        return false;
    }
}
