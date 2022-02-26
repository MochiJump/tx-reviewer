package com.mochijump.reviewer.values;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties("mochijump.global")
@Data
@Slf4j
public class GlobalValues {
    // manual methods to mark
    public static final Set<String> HARVEST_REWARDS_METHOD_NAMES = Set.of();
    public static final Set<String> EXIT_LP_METHOD_NAMES = Set.of();

    public static final Set<String> METHODS_THAT_CAUSE_MISSING_INFO = Set.of();

    private Set<String> harvestRewardsMethodNames = HARVEST_REWARDS_METHOD_NAMES;
    private Set<String> exitLpMethodNames = EXIT_LP_METHOD_NAMES;
    private Set<String> methodsThatCauseMissingInfo = METHODS_THAT_CAUSE_MISSING_INFO;

    //manual tx overrides
    public static Map<String, Double> TRX_NATIVE_LOSS_OVERRIDE = Map.of();

    public static Map<String, Double> TRX_NATIVE_BORROW_OVERRIDE = Map.of();

    public static Map<String, Double> TRX_NATIVE_COLLATERAL_WITHDRAW_OVERRIDE = Map.of();

    public static Map<String, Double> TRX_NATIVE_AMOUNT_FROM_LP_EXIT = Map.of();

    public static Map<String, Double> TRX_NATIVE_BUY_RETURN_MISSING = Map.of();

    public static List<String> CONTRACT_IGNORE_COMPLETELY = List.of();

    public static List<String> TRX_IGNORE_COMPLETELY = List.of();

    private Map<String, Double> txNativeLossOverride = TRX_NATIVE_LOSS_OVERRIDE;
    private Map<String, Double> txNativeBuyReturnMissing = TRX_NATIVE_BUY_RETURN_MISSING;
    private List<String> contractIgnoreCompletely = CONTRACT_IGNORE_COMPLETELY;
    private List<String> txIgnoreCompletely = TRX_IGNORE_COMPLETELY;
    private Map<String, Double> txNativeBorrowOverride = TRX_NATIVE_BORROW_OVERRIDE;
    private Map<String, Double> txNativeCollateralWithdrawOverride = TRX_NATIVE_COLLATERAL_WITHDRAW_OVERRIDE;
    private Map<String, Double> txNativeAmountFromLpExit = TRX_NATIVE_AMOUNT_FROM_LP_EXIT;

    /**
     * To manually link a transaction to an outcome (compares the Double to the amount of native coin used in the
     * provided tx hash)
     */
    private List<String> originationWallets = new ArrayList<>();

    /**
     * Your wallet
     */
    private String wallet;

    /**
     * Gemini csv reader
     */

    private String geminiCsvLoc;

    /**
     * OnChain reader (yaml won't allow tuples so I'm stuck with a map. Probably will need to revist this)
     */
    private Map<String, List<String>> onChainCsvFiles;

    @PostConstruct
    public void validation(){
        if (onChainCsvFiles == null){
            throw new IllegalStateException("mochijump.global.onChainCsvFiles must be set, at least for now");
        }

        if (originationWallets.size()==0){
            log.warn("Warning you have not marked any wallets as owned origination wallets");
        }

        if (StringUtils.isEmpty(wallet)){
            throw new IllegalStateException("mochijump.global.wallet must be set to the wallet being analyzed");
        }
    }
}
