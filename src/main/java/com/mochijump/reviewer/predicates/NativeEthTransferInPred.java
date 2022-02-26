package com.mochijump.reviewer.predicates;

import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@AllArgsConstructor
@Component
@Slf4j

public class NativeEthTransferInPred implements Predicate<PredicateDTO> {
    public StateMaintainer stateMaintainer;
    public GlobalValues values;

    @Override
    public boolean test(PredicateDTO dto) {
        final String ownerWallet = stateMaintainer.getOwnershipState().getWallet();
        if (
                !StringUtils.equals(dto.getFrom(), ownerWallet) &&
                        StringUtils.equals(dto.getTo(), ownerWallet) &&
                        dto.getErc20Interactions().isEmpty() &&
                        dto.getNativeCoinInAmount() > 0 &&
                        dto.getNativeCoinOutAmount() == 0
        ) {
            // TODO add an option to add wallet to origination wallet at runtime if not recognized?
            // there are two possibilities here:
            // 1. this is coming from a source that you already own
            if (values.getOriginationWallets().contains(dto.getFrom())) {
                // in this case we shall skip any action and just return true
                log.info("Skipping tx={} as this tx came from={} which is an origination wallet", dto.getTxHash(),
                         dto.getFrom());
                return true;
            }
            // 2. this is new and you have to report it as a taxable gain with cost basis of 0
            log.info("Creating a zero cost basis add as this tx={} came from={} which is an origination wallet",
                     dto.getTxHash(), dto.getFrom());
            addBasisOfZero(dto);
            return true;
        } else {
            return false;
        }
    }

    public void addBasisOfZero(PredicateDTO dto) {
        stateMaintainer.addZeroCostBasis(dto.getNativeCoin(), dto.getTimeOfTx(), dto.getNativeCoinInAmount());
    }
}
