package ie.universityofgalway.finance.marketdata.infrastructure;

import ie.universityofgalway.finance.marketdata.domain.PriceHistoryProvider;
import ie.universityofgalway.finance.marketdata.domain.PricePoint;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Profile("stub")
public class StubPriceHistoryProvider implements PriceHistoryProvider {

    @Override
    public Optional<List<PricePoint>> findDailyHistory(String symbol) {
        // Stub mode has no historical data source.
        return Optional.empty();
    }
}