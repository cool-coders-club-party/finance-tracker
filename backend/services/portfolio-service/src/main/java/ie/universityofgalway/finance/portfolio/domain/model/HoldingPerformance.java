package ie.universityofgalway.finance.portfolio.domain.model;

import java.math.BigDecimal;

/**
 * Profit and loss for one stock, as calculated by
 * {@link ie.universityofgalway.finance.portfolio.domain.service.PerformanceCalculator}
 * from that stock's trade history.
 *
 * @param ticker               the stock symbol
 * @param quantity             shares still held; zero for a closed position
 * @param avgPurchasePrice     weighted average price paid per share held
 * @param currentPrice         latest market price, or {@code null} for a closed position
 * @param costBasis            {@code avgPurchasePrice * quantity}
 * @param marketValue          {@code currentPrice * quantity}
 * @param unrealizedPnl        {@code marketValue - costBasis}
 * @param unrealizedPnlPercent {@code unrealizedPnl} as a percentage of {@code costBasis}
 * @param realizedPnl          profit or loss locked in by past sales
 */
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