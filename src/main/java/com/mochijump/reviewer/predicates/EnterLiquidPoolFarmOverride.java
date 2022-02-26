package com.mochijump.reviewer.predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class EnterLiquidPoolFarmOverride extends IsOverride {
    private final StateMaintainer maintainer;
    private final ObjectMapper mapper;
//    private final GlobalValues values;


    /**
     * For some farm entries they will match with TransferErc20ToSell and that is fine, we don't want to record this
     * as an exchange.
     * <p>
     * If, however, doing so results in awards (e.g. Sushi's double coin rewards [arguably bad design on sushi's
     * part]), then we need to record what we got back correctly with zero cost basis
     *
     * @param dto
     * @return
     */
    @Override
    public boolean test(PredicateDTO dto) {
        final String ownerWallet = maintainer.getOwnershipState().getWallet();
        if (
                StringUtils.equals(dto.getFrom(), ownerWallet) &&
                        !StringUtils.equals(dto.getTo(), ownerWallet) &&
                        dto.getNativeCoinInAmount() == 0 &&
                        dto.getNativeCoinOutAmount() == 0 &&
                        !dto.getErc20Interactions().isEmpty() &&
                        dto.getErc20Interactions().size() >= 1 &&
                        //todo make configurable list to check from
                        dto.getMethod().matches("Deposit")
        ) {
            for (final PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                if (
                        StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                                !StringUtils.equals(interaction.getTo(), ownerWallet)
                ) {
                    //This is the lp token leaving:
                    log.info(
                            "Determined this is an add to an already entered liquidity pool that returns rewards on "
                                    + "adds dto={}",
                            mapper.valueToTree(dto));
                    // we're only recording the loss due to gas paid
                    maintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                               dto.getTxCostAtTheTime() / dto.getNativeCoinHistoricalDollarPerUnit());
                }
                if (
                        !StringUtils.equals(interaction.getFrom(), ownerWallet) &&
                                StringUtils.equals(interaction.getTo(), ownerWallet)
                ) {
                    //rewards here zero cost basis
                    maintainer.addZeroCostBasis(interaction.getCoinSymbol(), dto.getTimeOfTx(),
                                                interaction.getErc20Amount());
                }
            }
            return true;
        }
        return false;
    }
}
