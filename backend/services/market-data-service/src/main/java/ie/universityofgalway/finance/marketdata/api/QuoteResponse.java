package ie.universityofgalway.finance.marketdata.api;

import java.math.BigDecimal;
import java.time.Instant;


/**
 * Public JSON quote returned by GET /api/quotes/{symbol}.
 *
 * @param symbol provider ticker symbol
 * @param price latest price in the currency supplied in the quote details
 * @param asOf provider timestamp of the price
 * @param stale whether this is a cached fallback after a provider failure
 * @param details optional instrument metadata and daily statistics; may be null
 */
public record QuoteResponse (
    String symbol,      // e.g AAPL
    BigDecimal price,   // exact money value
    Instant asOf,       // time supplied by the price provider
    boolean stale,      // true when an older quote is used after a provider failure
    QuoteDetailResponse details // detailed ticker call, returns more relevant data for stock card
) {

}

