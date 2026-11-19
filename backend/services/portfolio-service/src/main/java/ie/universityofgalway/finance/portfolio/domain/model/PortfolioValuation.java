package ie.universityofgalway.finance.portfolio.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioValuation(List<HoldingValuation> holdings, String currency, boolean complete,
                                 boolean stale, int unavailableHoldings, BigDecimal costBasis,
                                 BigDecimal currentValue, BigDecimal unrealisedGainLoss) {
}
