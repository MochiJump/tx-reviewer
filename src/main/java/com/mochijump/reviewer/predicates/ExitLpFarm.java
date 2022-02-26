package com.mochijump.reviewer.predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ExitLpFarm implements Predicate<PredicateDTO> {
    private GlobalValues values;
    private StateMaintainer maintainer;

    private ObjectMapper mapper;


    @Override
    public boolean test(final PredicateDTO dto) {
        if (values.getExitLpMethodNames().contains(dto.getMethod())) {
            // we have to look up the LP we previously deposited, this is tricky if we have more than one we're in...
            // I think this requires another manual intervention
            Boolean lpTokensInvolved = false;
            for (PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                if (maintainer.getOwnershipState().getMasterSet().stream().anyMatch(
                        entry -> StringUtils
                                .equals(entry.getCoinName(), interaction.getContractAddressOfCoin()))) {
                    // this is just a transfer back to the actual wallet, only accept loss for gas
                    maintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                               dto.getTxCostAtTheTime() / dto.getNativeCoinHistoricalDollarPerUnit());
                    lpTokensInvolved = true;
                } else {
                    //case where we get rewards which is a zero cost basis
                    maintainer.addZeroCostBasis(interaction.getCoinSymbol(), dto.getTimeOfTx(),
                                                interaction.getErc20Amount());
                }
            }
            return lpTokensInvolved;
        }
        return false;
    }
}
