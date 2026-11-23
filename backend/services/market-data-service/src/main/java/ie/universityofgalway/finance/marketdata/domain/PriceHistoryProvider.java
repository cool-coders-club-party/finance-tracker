package ie.universityofgalway.finance.marketdata.domain;

import java.util.List;
import java.util.Optional;

/**
 * Port for retrieving daily closing prices independently of the history source.
 */
public interface PriceHistoryProvider {
    /**
     * Finds available daily prices for a normalised ticker.
     *
     * @param symbol non-null ticker, trimmed and uppercased by the caller
     * @return daily prices ordered oldest first, or an empty optional if no history is available
     * @throws MarketDataUnavailableException if the provider request fails or returns unusable history
     */
    Optional<List<PricePoint>> findDailyHistory(String symbol);
}