package ie.universityofgalway.finance.marketdata.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A market price with its original provider timestamp and optional statistics.
 *
 * @param symbol provider ticker symbol
 * @param price latest price in the currency supplied in the quote details
 * @param asOf provider timestamp of the price
 * @param stale whether this is a cached fallback after a provider failure
 * @param details optional instrument metadata and daily statistics; may be null
 */
public record Quote(
    String symbol,
    BigDecimal price,
    Instant asOf,
    boolean stale,
    QuoteDetails details
) {
    /**
     * Creates a quote without optional statistics, marked as fresh.
     *
     * @param symbol provider ticker symbol
     * @param price latest price in the currency supplied in the quote details
     * @param asOf provider timestamp of the price
     */
    public Quote(String symbol, BigDecimal price, Instant asOf) {
        this(symbol, price, asOf, false, null);
    }

    /**
     * Creates a quote without optional statistics.
     *
     * @param symbol provider ticker symbol
     * @param price latest price in the currency supplied in the quote details
     * @param asOf provider timestamp of the price
     * @param stale whether this is a cached fallback after a provider failure
     */
    public Quote(String symbol, BigDecimal price, Instant asOf, boolean stale) {
        this(symbol, price, asOf, stale, null);
    }

    /**
     * Copies this quote as a stale fallback without changing its price, timestamp or statistics.
     *
     * @return a quote with {@code stale} set to true
     */
    public Quote asStale() {
        return new Quote(symbol, price, asOf, true, details);
    }
}

