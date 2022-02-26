package com.mochijump.reviewer.predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

@AllArgsConstructor
@Component
@Slf4j
public class ExitNativeCollateralPositionOverride extends IsOverride {
    private GlobalValues values;
    private StateMaintainer maintainer;

    private ObjectMapper mapper;


    @Override
    public boolean test(final PredicateDTO dto) {
        if (dto.getMethod().equals("Withdraw ETH")) {

            boolean collateralTokenReturned = false;
            for (PredicateDTO.Erc20Interaction interaction : dto.getErc20Interactions()) {
                if (interaction.getCoinSymbol().startsWith("am") && maintainer.getOwnershipState().getMasterSet()
                                                                              .stream().anyMatch(
                                entry -> StringUtils
                                        .equals(entry.getCoinName(), interaction.getCoinSymbol()))) {
                    // gas
                    maintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                               dto.getTxCostAtTheTime() / dto.getNativeCoinHistoricalDollarPerUnit());
                    collateralTokenReturned = true;
                    Optional<Double> nativeAmountReturned =
                            Optional.ofNullable(values.getTxNativeCollateralWithdrawOverride().get(dto.getTxHash()));
                    if (nativeAmountReturned.isPresent()) {
                        Double value = dto.getNativeCoinHistoricalDollarPerUnit()
                                * nativeAmountReturned.get();
                        maintainer.addValueToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                                    value,
                                                    nativeAmountReturned.get());
                        // remove the same value and actual amount from the collateral token
                        maintainer.removeValueFromMaster(interaction.getCoinSymbol(), dto.getTimeOfTx(),
                                                         interaction.getErc20Amount(), value);
                    } else {
                        throw new IllegalStateException(
                                String.format(
                                        "Sorry, this is dumb, but the data for incoming native coin amount when "
                                                + "exiting a"
                                                + " native collateral position is missing from provided data, please"
                                                + " lookup this tx=%s and add "
                                                + "tx and native coin amount to txNativeBorrowOverride",
                                        dto.getTxHash()));
                    }
                } else {
                    //TODO not sure this ever happens..
                    //case where we get rewards which is a zero cost basis
                    maintainer.addZeroCostBasis(interaction.getCoinSymbol(), dto.getTimeOfTx(),
                                                interaction.getErc20Amount());
                }
                if (!collateralTokenReturned) {
                    throw new IllegalStateException(
                            String.format(
                                    "txHash=%s return of collateral detected, however, cannot detect a return of "
                                            + "collateral token",
                                    dto.getTxHash()));
                }
                return true;
            }
        }
        return false;
    }
}
