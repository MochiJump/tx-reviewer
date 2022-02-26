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
public class TransferErc20ToSelf implements Predicate<PredicateDTO> {
    public StateMaintainer stateMaintainer;
    public ObjectMapper mapper;

    @Override
    public boolean test(PredicateDTO dto) {
        final String ownerWallet = stateMaintainer.getOwnershipState().getWallet();
        //TODO should have override to report sale
        if (
                StringUtils.equals(dto.getFrom(), ownerWallet) &&
                        !StringUtils.equals(dto.getTo(), ownerWallet) &&
                        dto.getNativeCoinInAmount() == 0 &&
                        dto.getNativeCoinOutAmount() == 0 &&
                        !dto.getErc20Interactions().isEmpty() &&
                        dto.getErc20Interactions().size() ==1
        ) {
            for (final PredicateDTO.Erc20Interaction interaction: dto.getErc20Interactions()){
                if (
                        StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                                !StringUtils.equals(interaction.getTo(), ownerWallet)
                ){
                    log.info("Determined this is a transfer and is still owned by user dto={}",
                             mapper.valueToTree(dto));
                    // we're only recording the loss due to gas paid
                    stateMaintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                                    dto.getTxCostAtTheTime() / dto.getNativeCoinHistoricalDollarPerUnit());
                    return true;
                }
            }
        }
        return false;
    }
}
