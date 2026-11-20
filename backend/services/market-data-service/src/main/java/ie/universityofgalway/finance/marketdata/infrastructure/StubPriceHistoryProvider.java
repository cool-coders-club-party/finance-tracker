package ie.universityofgalway.finance.marketdata.infrastructure;

import ie.universityofgalway.finance.marketdata.domain.PriceHistoryProvider;
import ie.universityofgalway.finance.marketdata.domain.PricePoint;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Disables historical prices in the stub profile without calling an external service.
 */
@Component
@Profile("stub")
public class StubPriceHistoryProvider implements PriceHistoryProvider {

    /**
     * Reports that no historical source is configured in stub mode.
     *
     * @param symbol ticker, ignored in stub mode
     * @return an empty optional
     */
    @Override
    public Optional<List<PricePoint>> findDailyHistory(String symbol) {
        // Stub mode has no historical data source.
        return Optional.empty();
    }
}