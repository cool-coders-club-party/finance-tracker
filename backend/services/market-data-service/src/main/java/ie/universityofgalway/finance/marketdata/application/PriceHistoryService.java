package ie.universityofgalway.finance.marketdata.application;

import ie.universityofgalway.finance.marketdata.domain.PriceHistoryProvider;
import ie.universityofgalway.finance.marketdata.domain.PricePoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Normalises caller-supplied tickers before requesting daily closing-price history from a provider.
 */
@Service
public class PriceHistoryService {
    private final PriceHistoryProvider provider;

    /**
     * Creates the application service.
     *
     * @param provider provider selected by the active Spring configuration
     */
    public PriceHistoryService(PriceHistoryProvider provider) {
        this.provider = provider;
    }

    /**
     * Trims the ticker and uppercases it using {@link Locale#ROOT} before requesting history.
     *
     * @param rawSymbol caller-supplied ticker; may be null or blank
     * @return daily prices, or an empty optional for null, blank or symbols with no history
     * @throws ie.universityofgalway.finance.marketdata.domain.MarketDataUnavailableException if the history
     *     provider is unavailable or returns unusable data
     */
    public Optional<List<PricePoint>> getHistory(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return Optional.empty();
        }

        // Use the same ticker format as quote lookup.
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        return provider.findDailyHistory(symbol);
    }
}
