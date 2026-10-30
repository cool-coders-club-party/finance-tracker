package ie.universityofgalway.finance.marketdata.domain;

import java.util.List;
import java.util.Optional;

public interface PriceHistoryProvider {
    // Empty Optional means the symbol has no history.
    Optional<List<PricePoint>> findDailyHistory(String symbol);
}