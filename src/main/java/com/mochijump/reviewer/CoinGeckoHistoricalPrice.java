package com.mochijump.reviewer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochijump.reviewer.interfaces.HistoricalPriceLookUp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class CoinGeckoHistoricalPrice implements HistoricalPriceLookUp {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final List<CoinListEntry> coinListEntries;
    // hopefully saves us from redoing a bunch of redundant calls;
    private Map<String, Double> cache = new HashMap<>();

    public CoinGeckoHistoricalPrice(final RestTemplate restTemplate, final ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.coinListEntries = importCoinList();
    }

    @Override
    @SneakyThrows
    public Double lookupPriceWhen(final Instant instant, final String rawCoinSymbol) {
        //need to normalize for looking up via coingecko's table, should not cause false positives, but good to test
        // this extensively
        final String coinSymbol = rawCoinSymbol.toUpperCase();
        final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        final String formattedDate = formatter.format(Date.from(instant));
        final List<CoinListEntry> coinListEntry = coinListEntries;
        final Optional<CoinListEntry> correspondingEntryOpt =
                coinListEntry.stream().filter(ce -> ce.getSymbol().equals(coinSymbol)).findFirst();
        if (correspondingEntryOpt.isPresent()) {
            final CoinListEntry coinEntry = correspondingEntryOpt.get();
            final String urlString =
                    "https://api.coingecko.com/api/v3/coins/" + coinEntry.getId() + "/history?date=" + formattedDate;
            final Optional<Double> cached =  Optional.ofNullable(cache.get(urlString));
            if (cached.isPresent()){
                return cached.get();
            }
            final URI url = URI.create(urlString);
            final RequestEntity re = new RequestEntity(HttpMethod.GET, url);
            ResponseEntity<JsonNode> result;
            try {
                result = restTemplate.exchange(re, JsonNode.class);
            } catch (HttpStatusCodeException hsce) {
                if (hsce.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
                    //TODO better to poll more often or just try to time when the api will let us continue?
                    log.info("coingecko is rate limiting us. Sleeping for 1 minute 10 seconds then continuing");
                    //TODO better way to wait then Thread.sleep
                    Thread.sleep(70000);
                    return lookupPriceWhen(instant, rawCoinSymbol);
                }
                log.error("encountered hsce when looking up coin=" + coinSymbol);
                throw hsce;
            }
            final Optional<JsonNode> marketData = Optional.ofNullable(result.getBody().get("market_data"));
            if (marketData.isEmpty()) {
                throw new IllegalStateException(
                        String.format(
                                "the coin=%s is listed, however, returns incomplete data, need to map this to a "
                                        + "different coin that will work with coingecko (i.e. amWMATIC to matic)",
                                coinSymbol));
            } else {
                Double historicalPrice = marketData.get().get("current_price").get("usd").asDouble();
                //TODO cache size limit should be configurable
                if (cache.size()>20){
                    //evict cache
                    cache = new HashMap<>();
                }
                cache.put(urlString, historicalPrice);
                return historicalPrice;
            }
        } else {
            log.info(
                    "Unable to find pricing for coin={} this makes sense for situations like liquidity tokens "
                            + "returning null to throw npe to ensure the historical price is never used",
                    coinSymbol);
            // an anti-pattern but it'll do for now
            return null;
        }
    }

    @SneakyThrows
    private List<CoinListEntry> importCoinList() {
        final String string = IOUtils.toString((new ClassPathResource("coinGeckoCoinlist.json")).getInputStream(),
                                               Charset.defaultCharset());
        final List<CoinListEntry> results = Arrays.asList(mapper.readValue(string, CoinListEntry[].class));
        //TODO override entries for situation where coingecko fails :/ so many outside information sources don't work
        // as expected.. make this a configuration...
        for (CoinListEntry result : results) {
            result.setSymbol(result.getSymbol().toUpperCase());
            if (result.getSymbol().equals("AMWMATIC")) {
                result.setId("wmatic");
            }
            // TODO I think this will even out? but it's a shame that coingecko's apis don't do what they say they will
            if (result.getSymbol().equals("DQUICK")) {
                result.setId("quick");
            }
        }
        return results;
    }


    @AllArgsConstructor
    @Builder
    @Data
    @NoArgsConstructor
    private static class CoinListEntry {
        private String id;
        private String symbol;
        private String name;
    }

}
