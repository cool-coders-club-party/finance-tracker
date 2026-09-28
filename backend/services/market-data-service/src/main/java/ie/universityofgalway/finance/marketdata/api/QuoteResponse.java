package ie.universityofgalway.finance.marketdata.api;

import java.math.BigDecimal;
import java.time.Instant;


//The JSON returned by GET /api/quotes/{symbol}
public record QuoteResponse (
    String symbol,      // e.g AAPL
    BigDecimal price,   // exact money value
    Instant asOf        // when price was fetched
) {

}

