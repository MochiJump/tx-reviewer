package com.mochijump.reviewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.CexReader;
import com.mochijump.reviewer.interfaces.ChainAnalyzer;
import com.mochijump.reviewer.interfaces.ChainReader;
import com.mochijump.reviewer.interfaces.HistoricalPriceLookUp;
import com.mochijump.reviewer.interfaces.StateMaintainer;
import com.mochijump.reviewer.values.GlobalValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@ConfigurationPropertiesScan
@SpringBootApplication
public class TransactionReviewerApplication implements CommandLineRunner {

    @Autowired
    private Orchestrator orchestrator;

    public static void main(String[] args) {
        new SpringApplicationBuilder(TransactionReviewerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Override
    public void run(String... args) {
        orchestrator.orchestrate();
    }

    /**
     * All of these bean loaders can be made to lookup which to use from a config once we start using different
     * implementations of these interfaces
     * <p>
     * This also clearly shows how the dependencies are being handled by spring for those unfamiliar
     */


    @Bean
    public CexReader cexReader(
            final StateMaintainer stateMaintainer, final ReviewerProperties properties, final GlobalValues values) {
        return new GeminiCsvReader(properties, stateMaintainer, values);
    }

    @Bean
    public ChainReader chainReader(
            final ObjectMapper mapper, final HistoricalPriceLookUp lookUp,
            final CSVChainReaderPredicateAnalyzer CSVChainReaderPredicateAnalyzer, final GlobalValues values) {
        return new CsvChainReader(mapper, lookUp, CSVChainReaderPredicateAnalyzer, values);
    }

    @Bean
    public StateMaintainer stateMaintainer(
            final ObjectMapper mapper, final HistoricalPriceLookUp lookUp, final
    GlobalValues values) {
        return new StateMaintainerImpl(mapper, lookUp, values);
    }

    @Bean
    public HistoricalPriceLookUp historicalPriceLookUp(final ObjectMapper mapper, final RestTemplate restTemplate) {
        return new CoinGeckoHistoricalPrice(restTemplate, mapper);
    }

//    TODO the dependencies on this object are too large, this we should recreate the list and pull from that :)
//    @Bean
//    public ChainAnalyzer chainAnalyzer(){
//        return new CSVChainReaderPredicateAnalyzer()
//    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
