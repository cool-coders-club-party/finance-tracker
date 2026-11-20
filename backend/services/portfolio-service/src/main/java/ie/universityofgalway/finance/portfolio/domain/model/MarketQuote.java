package ie.universityofgalway.finance.portfolio.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A market snapshot kept separate from a holding's recorded purchase cost.
 *
 * @param symbol quoted stock symbol
 * @param price quoted unit price
 * @param currency provider currency code; may be null when metadata is missing
 * @param asOf provider timestamp of the price
 * @param stale whether Market Data used a cached fallback
 */
public record MarketQuote(String symbol, BigDecimal price, String currency, Instant asOf, boolean stale) {
}
