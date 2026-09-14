package ie.universityofgalway.finance.marketdata.domain;

import java.util.Optional;

public interface MarketDataProvider {
    //Optional wraps the possible quote, it makes the "nothing was found" case explicit
    //instead of returning `null`
    Optional<Quote> findQuote(String symbol);
}
