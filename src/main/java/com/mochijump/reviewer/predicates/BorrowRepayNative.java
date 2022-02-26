package com.mochijump.reviewer.predicates;

import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;


@AllArgsConstructor
@Component
@Slf4j
/**
 * TODO tech debt: break into two different classes
 */
public class BorrowRepayNative implements Predicate<PredicateDTO> {
    private StateMaintainer maintainer;
    private GlobalValues globalValues;

    /**
     * This class is a giant mess, lol, fix it. It should be a situation where throws exception if not in list with
     * instructions because data is missing from csv files
     *
     * @param dto
     * @return
     */
    @Override public boolean test(PredicateDTO dto) {
        //TODO document this short cut!!
        // Currently assigning value of 0 on both in and out which will roughly balance for short lived longs.
        // There must be a more precise way of handling this, perhaps a different stateful object?
        final boolean borrow = dto.getMethod().equals("Borrow ETH");
        final boolean repay = dto.getMethod().equals("Repay ETH");
        if (borrow || repay) {
            maintainer.addLossToMaster(dto.getNativeCoin(), dto.getTimeOfTx(),
                                       dto.getTxCostAtTheTime() / dto.getNativeCoinHistoricalDollarPerUnit());
            if (borrow) {
                //must be in override list
                Optional<Double> amount =
                        Optional.ofNullable(globalValues.getTxNativeBorrowOverride().get(dto.getTxHash()));
                if (amount.isPresent()) {
                    //TODO this shouldn't be evaluaed for a gain at all
                    // TODO in the short term this will skip the addZeroCostBasis because we don't want the in to be
                    //  taxable
                    maintainer.addValueToMaster(dto.getNativeCoin(), dto.getTimeOfTx(), 0D, amount.get());
                } else {
                    throw new IllegalStateException(
                            String.format(
                                    "Sorry, this is dumb, but the data for incoming native coin amount when borrowing"
                                            + " native "
                                            + "coins is missing from provided data, please lookup this tx=%s and add "
                                            + "tx and native coin amount to txNativeBorrowOverride",
                                    dto.getTxHash()));
                }
            } else {
                //TODO this shouldn't be evalutated for a loss at all
                maintainer.removeValueFromMaster(dto.getNativeCoin(), dto.getTimeOfTx(), dto.getNativeCoinOutAmount(),
                                                 0D);

            }
            return true;
        }
        return false;
    }
}
