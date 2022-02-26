package com.mochijump.reviewer.predicates;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class PredicateDTO {
    private String txHash;
    private Instant timeOfTx;
    private String from;
    private String to;
    private Double nativeCoinInAmount;
    private Double nativeCoinOutAmount;
    private Double txCostAtTheTime;
    private Double nativeCoinHistoricalDollarPerUnit;
    Set<Erc20Interaction> erc20Interactions;
    private String method;
    private String nativeCoin;
    private String error;

    @Builder
    @Data
    public static class Erc20Interaction {
        private String from;
        private String to;
        private Double erc20Amount;
        private Double historicalPrice;
        private String coinSymbol;
        private String contractAddressOfCoin;
    }


}
