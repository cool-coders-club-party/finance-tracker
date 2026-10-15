package ie.universityofgalway.finance.marketdata.domain;

import java.math.BigDecimal;
import java.time.Instant;

//Represents a market price and whether it came from stale cache data.
public record Quote (String symbol, BigDecimal price, Instant asOf, boolean stale) {

    //Keeps existing three-argument quote creation working and marks it as fresh.
    public Quote(String symbol, BigDecimal price, Instant asOf) {
        this(symbol, price, asOf, false);
    }

    //Creates a copy of this quote marked as stale.
    public Quote asStale() {
        return new Quote(symbol, price, asOf, true);
    }

}

