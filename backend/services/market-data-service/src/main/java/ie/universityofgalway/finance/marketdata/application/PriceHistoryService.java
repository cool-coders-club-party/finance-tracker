package ie.universityofgalway.finance.marketdata.application;

import ie.universityofgalway.finance.marketdata.domain.PriceHistoryProvider;
import ie.universityofgalway.finance.marketdata.domain.PricePoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PriceHistoryService {
    private final PriceHistoryProvider provider;

    public PriceHistoryService(PriceHistoryProvider provider) {
        this.provider = provider;
    }

    public Optional<List<PricePoint>> getHistory(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return Optional.empty();
        }

        // Use the same ticker format as quote lookup.
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        return provider.findDailyHistory(symbol);
    }
}