package ie.universityofgalway.finance.marketdata.domain;

import java.math.BigDecimal;
import java.time.Instant;

// A price, its optional details, and whether cached data is being used.
public record Quote(
    String symbol,
    BigDecimal price,
    Instant asOf,
    boolean stale,
    QuoteDetails details
) {
    // Existing stub and tests can still create a basic fresh quote.
    public Quote(String symbol, BigDecimal price, Instant asOf) {
        this(symbol, price, asOf, false, null);
    }

    // Keep the existing four-argument constructor working.
    public Quote(String symbol, BigDecimal price, Instant asOf, boolean stale) {
        this(symbol, price, asOf, stale, null);
    }

    // A cached fallback retains all the original quote details.
    public Quote asStale() {
        return new Quote(symbol, price, asOf, true, details);
    }
}

