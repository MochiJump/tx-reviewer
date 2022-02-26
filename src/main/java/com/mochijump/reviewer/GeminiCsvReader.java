package com.mochijump.reviewer;

import com.mochijump.reviewer.interfaces.CexReader;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;


@AllArgsConstructor
@Slf4j
public class GeminiCsvReader implements CexReader {
    private ReviewerProperties properties;
    private StateMaintainer stateMaintainer;
    private GlobalValues globalValues;

    public static final DecimalFormat currencyDecimalFormat = (DecimalFormat) NumberFormat.getCurrencyInstance();
    {
        currencyDecimalFormat.setNegativePrefix("($");
        currencyDecimalFormat.setNegativeSuffix(")");
    }

    @Override
    @SneakyThrows
    public void updateOwnershipWithCexData() {
        List<String[]> allData = CSVHelper.loadCSVFile(globalValues.getGeminiCsvLoc());
        for (String[] row : allData) {
            final String type = row[2];
            if (!(StringUtils.equalsIgnoreCase("BUY", type) || StringUtils.equalsIgnoreCase("SELL", type))){
                //just skip any line that isn't a buy or sell
                continue;
            }
            //todo bad var name
            final String coinNameWithCurrency = row[3];
            final String coinName = coinNameWithCurrency.replace(properties.getCurrencyString(), "");
            final Instant inst = Instant.parse(row[0] + "T" + row[1] + "Z");
            if (StringUtils.equalsIgnoreCase("BUY", type)) {
                // make the Pair for amount spent vs amount bought:
                Double loss = 0D;
                if (StringUtils.isNotEmpty(row[8])) {
                    loss = currencyDecimalFormat.parse(row[8]).doubleValue();
                }
                Double totalSpent = (currencyDecimalFormat.parse(row[7]).doubleValue() + loss) * -1;
                Double amountPurchased = Double.valueOf(row[getColumnfromWhatString(coinName)].replaceAll("\\s.*", ""));
                stateMaintainer.addValueToMaster(coinName, inst, totalSpent, amountPurchased);
            }
            if (StringUtils.equalsIgnoreCase("SELL", type)) {
                final Double amountToSell =
                        Double.valueOf(row[getColumnfromWhatString(coinName)].replaceAll("\\s.*\\)", "").replace("(", ""));
                final Double valueOfSale = currencyDecimalFormat.parse(row[7]).doubleValue();
                stateMaintainer.removeValueFromMaster(coinName, inst, amountToSell, valueOfSale);
            }
        }
    }

    private int getColumnfromWhatString(final String what) {
        // TODO not low hanging fruit. This is not static, need to parse where each coin's add/loss column is
        switch (what) {
            case "ETH":
                //find eth;
                return 13;
            case "BTC":
                return 10;
            case "BAT":
                return 16;
            case "ENJ":
                return 19;
            case "MATIC":
                return 22;
            default:
                throw new IllegalStateException("This line should never be reached");
        }

    }
}
