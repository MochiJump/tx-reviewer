package com.mochijump.reviewer;

import com.mochijump.reviewer.models.OwnershipState;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class CSVHelper {

    @SneakyThrows
    public static List<String[]> loadCSVFile(final String filePath) {
        FileReader filereader = new FileReader(filePath);
        CSVReader csvReader = new CSVReaderBuilder(filereader)
                .withSkipLines(1)
                .build();
        return csvReader.readAll();
    }

    public static void saveMasterSetAsCsv(final OwnershipState state) {
        final List<String[]> csvRows = new ArrayList<>();
        state.getMasterSet().forEach(ms -> {
            csvRows.add(new String[]{
                    ms.getTime().toString(), ms.getCoinName(), ms.getVerb().toString(), ms.getValue().toString(),
                    ms.getAmount().toString()
            });
        });
        saveCsvFile(csvRows, Path.of("masterList.csv"));
    }

    public static void saveInFluxAsCsv(final OwnershipState state) {
        final List<String[]> csvRows = new ArrayList<>();
        state.getInFlux().entrySet().stream()
             .forEach(es -> {
                 es.getValue().forEach(entry -> {
                     csvRows.add(new String[]{
                             entry.getTime().toString(), es.getKey(), entry.getValue().toString(),
                             entry.getAmount().toString()
                     });
                 });
             });
        saveCsvFile(csvRows, Path.of("inFlux.csv"));
    }

    public static void saveShortTerm8949Format(final OwnershipState state) {
        final List<String[]> csvRows = new ArrayList<>();
        state.getShortTermIrsFormatList().stream()
             .forEach(format -> {
                 csvRows.add(new String[]{
                         format.getDescription(), format.getDateAcquired().toString(), format.getDateSold().toString(),
                         format.getProceeds().toString(), format.getCost().toString(), format.getGain().toString()
                 });
             });
        saveCsvFile(csvRows, Path.of("irs8949Short.csv"));
    }

    public static void saveLongTerm8949Format(final OwnershipState state) {
        final List<String[]> csvRows = new ArrayList<>();
        state.getLongTermIrsFormatList().stream()
             .forEach(format -> {
                 csvRows.add(new String[]{
                         format.getDescription(), format.getDateAcquired().toString(), format.getDateSold().toString(),
                         format.getProceeds().toString(), format.getCost().toString(), format.getGain().toString()
                 });
             });
        saveCsvFile(csvRows, Path.of("irs8949Long.csv"));
    }

    @SneakyThrows
    public static void saveCsvFile(List<String[]> stringArray, Path path) {
        CSVWriter writer = new CSVWriter(new FileWriter(path.toString()));
        writer.writeAll(stringArray);
        writer.close();
    }

}
