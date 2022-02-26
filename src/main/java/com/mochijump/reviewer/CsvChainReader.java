package com.mochijump.reviewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.ChainAnalyzer;
import com.mochijump.reviewer.interfaces.ChainReader;
import com.mochijump.reviewer.interfaces.HistoricalPriceLookUp;
import com.mochijump.reviewer.predicates.PredicateDTO;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mochijump.reviewer.values.DecimalFormats.nonCurrencyDecimalFormat;

@AllArgsConstructor
@Slf4j
public class CsvChainReader implements ChainReader {
    private ObjectMapper mapper;
    private HistoricalPriceLookUp historicalPriceLookUp;
    private ChainAnalyzer chainAnalyzer;
    private GlobalValues values;


    /**
     * I want to create scenarios for patterns between the master and erc20 csv files and if something new falls
     * out of that pattern throw an IAE so it can be addressed on it's own
     */


    @Override
    @SneakyThrows
    public void updateOwnershipWithChainData() {
        //step 1 start to collect information
        // each time in the list will be the master transaction list of a chain
        var onChainKeySet = values.getOnChainCsvFiles().entrySet();
        for (Map.Entry<String, List<String>> item : onChainKeySet) {
            // etherscan only spits out lists of ~5000, so we can have multiple additional csv files just to cover
            // the ecr20 transaction details of a single master transaction list csv file
            // First value will be the master tx list, anything else will be loaded as subtxs (only tests with erc20
            // data so far)
            List<String[]> masterList = CSVHelper.loadCSVFile(item.getValue().get(0));
            for (String[] masterRow : masterList) {
                // step 2 see if the transaction is findable in the suppl csv files
                // TODO only checking erc20 for now
                final String tx = masterRow[0];
                final Set<String[]> erc20subTxs = new HashSet<>();
                final ArrayList<String> withoutMaster = new ArrayList<>(item.getValue());
                withoutMaster.remove(0);
                withoutMaster
                        .stream()
                        .map(file -> CSVHelper.loadCSVFile(file)).forEach(streamListArray -> {
                            erc20subTxs.addAll(streamListArray.stream().filter(row -> row[0].equals(tx))
                                                              .collect(Collectors.toList()));
                        });

                //setup dto
                final Double priceAtTime = Double.valueOf(masterRow[12]);
                final Double txCostAtTime = Double.valueOf(masterRow[10]) * priceAtTime;
                Instant timeOfTx = Instant.ofEpochSecond(Long.valueOf(masterRow[2]));
                final PredicateDTO dto = PredicateDTO.builder()
                                                     .timeOfTx(timeOfTx)
                                                     .from(masterRow[4])
                                                     .to(masterRow[5])
                                                     .nativeCoinInAmount(Double.valueOf(masterRow[7]))
                                                     .nativeCoinOutAmount(Double.valueOf(masterRow[8]))
                                                     .txHash(masterRow[0])
                                                     .txCostAtTheTime(txCostAtTime)
                                                     .nativeCoinHistoricalDollarPerUnit(priceAtTime)
                                                     .method(masterRow[15])
                                                     .nativeCoin(item.getKey())
                                                     .error(masterRow[13])
                                                     .build();

                final Set<PredicateDTO.Erc20Interaction> erc20Interactions = new HashSet<>();
                erc20subTxs.forEach(subTx -> {
                    final Double historicalErc20Price;
                    final String coinSymbol = subTx[8];
                    if (StringUtils.equals(coinSymbol.substring(1), item.getKey())) {
                        historicalErc20Price = priceAtTime;
                    } else {
                        historicalErc20Price = historicalPriceLookUp.lookupPriceWhen(timeOfTx, subTx[8]);
                    }
                    erc20Interactions.add(
                            PredicateDTO.Erc20Interaction.builder()
                                                         .from(subTx[3])
                                                         .to(subTx[4])
                                                         .erc20Amount(runtimeParse(subTx[5]))
                                                         .historicalPrice(historicalErc20Price)
                                                         .coinSymbol(coinSymbol)
                                                         .contractAddressOfCoin(subTx[6])
                                                         .build());
                });
                dto.setErc20Interactions(erc20Interactions);
                log.info("Setup DTO={}", mapper.valueToTree(dto));
                chainAnalyzer.analyze(dto);
            }
        }
    }

    @SneakyThrows
    private Double runtimeParse(String value) {
        return nonCurrencyDecimalFormat.parse(value).doubleValue();
    }
}
