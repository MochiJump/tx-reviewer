package com.mochijump.reviewer.interfaces;

import java.time.Instant;

/**
 * @author aathi
 */
public interface HistoricalPriceLookUp {

    Double lookupPriceWhen(final Instant instant, final String coinSymbol);
}
