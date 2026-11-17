package ie.universityofgalway.finance.portfolio.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/** A read-only market snapshot, separate from the recorded cost of a holding. */
public record MarketQuote(String symbol, BigDecimal price, String currency, Instant asOf, boolean stale) {
}
