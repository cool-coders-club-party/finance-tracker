package ie.universityofgalway.finance.portfolio.domain.model;

import java.math.BigDecimal;

public record HoldingPerformance(
        String ticker,
        BigDecimal quantity,
        BigDecimal avgPurchasePrice,
        BigDecimal currentPrice,          // null when the position is closed
        BigDecimal costBasis,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercent,
        BigDecimal realizedPnl
) {}