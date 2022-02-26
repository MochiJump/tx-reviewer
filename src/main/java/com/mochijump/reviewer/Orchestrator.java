package com.mochijump.reviewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.CexReader;
import com.mochijump.reviewer.interfaces.ChainReader;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

@AllArgsConstructor
@Component
@Slf4j
public class Orchestrator {

    private final CexReader cexReader;
    private final ChainReader chainReader;
    private final StateMaintainer stateMaintainer;
    private final ObjectMapper mapper;
    private final GlobalValues globalValues;

    public void orchestrate() {
        //TODO wire up lookup (nothing happens currently)
        stateMaintainer.loadStateFromFile(Optional.empty());
        if (StringUtils.isNotEmpty(globalValues.getGeminiCsvLoc())) {
            cexReader.updateOwnershipWithCexData();
        } else{
            log.warn("No cex csv file location provided, skipping reading cex");
        }
        if (!CollectionUtils.isEmpty(globalValues.getOnChainCsvFiles())) {
            chainReader.updateOwnershipWithChainData();
        } else{
            log.warn("No on chain csv file locations provided, skipping reading on chain data");
        }

        log.info("////////$$$$$$$ chronological buy/sell result={}",
                 mapper.valueToTree(stateMaintainer.getOwnershipState().getMasterSet()));

        CSVHelper.saveMasterSetAsCsv(stateMaintainer.getOwnershipState());
        //do the final calculation
        stateMaintainer.finalizeMasterToInFlux();
        CSVHelper.saveShortTerm8949Format(stateMaintainer.getOwnershipState());
        CSVHelper.saveLongTerm8949Format(stateMaintainer.getOwnershipState());
        //logs
        log.info("////////$$$$$$$  results={}", mapper.valueToTree(stateMaintainer.getOwnershipState()));
        log.info("short term gain {}, long term gain {}", stateMaintainer.getOwnershipState().getShortTermGains(),
                 stateMaintainer.getOwnershipState().getLongTermGains());
        CSVHelper.saveInFluxAsCsv(stateMaintainer.getOwnershipState());
        Set<String> keys = stateMaintainer.getOwnershipState().getInFlux().keySet();
        for (String key : keys) {
            final List<Double> listDouble = stateMaintainer.getOwnershipState().getInFlux().get(key).stream()
                                                           .map(e -> e.getAmount()).collect(Collectors.toList());
            Double totalAmount = 0D;
            for (Double startAdding : listDouble) {
                totalAmount = totalAmount + startAdding;
            }
            log.info("total {} is {}", key, totalAmount);
        }


        //shut down
        System.exit(0);
    }
}
