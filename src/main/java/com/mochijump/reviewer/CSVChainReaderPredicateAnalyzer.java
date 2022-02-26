package com.mochijump.reviewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.ChainAnalyzer;
import com.mochijump.reviewer.predicates.AddERC20sToLiquidityPool;
import com.mochijump.reviewer.predicates.AddNativeToLiquidityPool;
import com.mochijump.reviewer.predicates.ApprovePredicate;
import com.mochijump.reviewer.predicates.BorrowRepayNative;
import com.mochijump.reviewer.predicates.EnterLiquidPoolFarmOverride;
import com.mochijump.reviewer.predicates.ExitLpFarm;
import com.mochijump.reviewer.predicates.ExitNativeCollateralPositionOverride;
import com.mochijump.reviewer.predicates.HarvestRewards;
import com.mochijump.reviewer.predicates.IgnoreTxCompletely;
import com.mochijump.reviewer.predicates.IsOverride;
import com.mochijump.reviewer.predicates.NativeEthTransferInPred;
import com.mochijump.reviewer.predicates.OverrideFailedTx;
import com.mochijump.reviewer.predicates.OverrideIgnoreContract;
import com.mochijump.reviewer.predicates.OverrideRequiredCheck;
import com.mochijump.reviewer.predicates.OverrideTxMissingReturnForNativeBuy;
import com.mochijump.reviewer.predicates.OverrideTxNativeLoss;
import com.mochijump.reviewer.predicates.PredicateDTO;
import com.mochijump.reviewer.predicates.RemoveERC20sFromLiquidityPool;
import com.mochijump.reviewer.predicates.RemoveNativeFromLiquidityPoolOverride;
import com.mochijump.reviewer.predicates.SwapTokenPred;
import com.mochijump.reviewer.predicates.TradeNativeForToken;
import com.mochijump.reviewer.predicates.TransferErc20ToSelf;
import com.mochijump.reviewer.predicates.TransferToSelf;
import com.mochijump.reviewer.predicates.TxCancelationAttempt;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@AllArgsConstructor
//TODO just mark as component for now
@Component
public class CSVChainReaderPredicateAnalyzer implements ChainAnalyzer {
    //TODO there's a better way to get all beans of type X
    private NativeEthTransferInPred nativeEthTransferInPred;
    private OverrideTxNativeLoss overrideTxNativeLoss;
    private TransferToSelf transferToSelf;
    private ApprovePredicate approvePredicate;
    private TransferErc20ToSelf transferErc20ToSelf;
    private SwapTokenPred swapTokenPred;
    private AddERC20sToLiquidityPool addERC20sToLiquidityPool;
    private HarvestRewards harvestRewards;
    private OverrideRequiredCheck overrideRequiredCheck;
    private OverrideTxMissingReturnForNativeBuy overrideTxMissingReturnForNativeBuy;
    private ExitLpFarm exitLpFarm;
    private OverrideFailedTx overrideFailedTx;
    private TradeNativeForToken tradeNativeForToken;
    private OverrideIgnoreContract overrideIgnoreContract;
    private BorrowRepayNative borrowRepayNative;
    private AddNativeToLiquidityPool addNativeToLiquidityPool;
    private RemoveERC20sFromLiquidityPool removeERC20SFromLiquidityPool;
    private IgnoreTxCompletely ignoreTxCompletely;
    private TxCancelationAttempt txCancelationAttempt;
    private ExitNativeCollateralPositionOverride exitNativeCollateralPositionOverride;
    private RemoveNativeFromLiquidityPoolOverride removeNativeFromLiquidityPoolOverride;
    private EnterLiquidPoolFarmOverride enterLiquidPoolFarmOverride;


    private ObjectMapper objectMapper;

    @SneakyThrows
    public void analyze(final PredicateDTO dto) {
        final List<Predicate<PredicateDTO>> list = getPredicateList();
        final List<Predicate<PredicateDTO>> processed = new ArrayList<>();
        for (Predicate<PredicateDTO> pred : list) {
            if (pred.test(dto)) {
                processed.add(pred);
                //stop processing if we encounter an override
                if (IsOverride.class.isAssignableFrom(pred.getClass())) {
                    break;
                }
            }
        }
//        if (dto.getTimeOfTx().isAfter(Instant.parse("2021-05-13T15:55:05Z")) && (dto.getErc20Interactions().stream()
//                                                                                    .anyMatch(in -> in.getCoinSymbol()
//                                                                                                      .startsWith(
//                                                                                                              "UNI")))) {
//            System.out.println("This block should be erased or commented out, only for debugging purposes");
//        }
        if (processed.size() > 1) {
            final StringBuilder builder = new StringBuilder();
            processed.forEach(entry -> builder.append(entry.getClass() + ","));
            throw new IllegalStateException(String.format(
                    "More than one predicate matched the dto! Issue with multiple passing predicates! classes=%s "
                            + "dto=%s",
                    builder.toString(), objectMapper.valueToTree(dto)));
        }
        if (processed.size() == 0) {
            throw new IllegalStateException(
                    String.format("None of the predicates matched this dto=%s", objectMapper.valueToTree(dto)));
        }
    }

    private List<Predicate<PredicateDTO>> getPredicateList() {
        //overrides must be in beginning of list and checks for needing overrides should come next
        return List
                .of(ignoreTxCompletely, overrideTxNativeLoss, overrideTxMissingReturnForNativeBuy, overrideFailedTx,
                    exitNativeCollateralPositionOverride, removeNativeFromLiquidityPoolOverride, overrideRequiredCheck,
                    enterLiquidPoolFarmOverride,
                    // all overrides must come before this check
                    overrideIgnoreContract,
                    // non override predicates can go here
                    nativeEthTransferInPred, transferToSelf, approvePredicate, transferErc20ToSelf, swapTokenPred,
                    addERC20sToLiquidityPool, harvestRewards, exitLpFarm, tradeNativeForToken, borrowRepayNative,
                    addNativeToLiquidityPool, removeERC20SFromLiquidityPool, txCancelationAttempt);
    }
}
