package ie.universityofgalway.finance.portfolio.api.dto;

import ie.universityofgalway.finance.portfolio.domain.model.HoldingPerformance;

import java.math.BigDecimal;

/**
 * Response item for {@code GET /api/portfolios/{id}/performance}: profit and loss for one stock.
 *
 * <p>A copy of {@link HoldingPerformance}, kept separate so the API shape can change without
 * touching the domain model. Money values are rounded to 2 decimal places.</p>
 *
 * @param ticker               the stock symbol
 * @param quantity             shares still held; zero for a closed position
 * @param avgPurchasePrice     weighted average price paid per share held (4 decimal places)
 * @param currentPrice         latest market price, or {@code null} for a closed position
 * @param costBasis            {@code avgPurchasePrice * quantity}
 * @param marketValue          {@code currentPrice * quantity}
 * @param unrealizedPnl        {@code marketValue - costBasis}: profit or loss if sold now
 * @param unrealizedPnlPercent {@code unrealizedPnl} as a percentage of {@code costBasis}
 * @param realizedPnl          profit or loss already locked in by past sales
 */
public record HoldingPerformanceResponse(
        String ticker,
        BigDecimal quantity,
        BigDecimal avgPurchasePrice,
        BigDecimal currentPrice,
        BigDecimal costBasis,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercent,
        BigDecimal realizedPnl
) {
    /**
     * Copies a domain result into the response shape.
     *
     * @param p the calculated performance for one stock
     * @return the response item
     */
    public static HoldingPerformanceResponse from(HoldingPerformance p) {
        return new HoldingPerformanceResponse(
                p.ticker(), p.quantity(), p.avgPurchasePrice(), p.currentPrice(),
                p.costBasis(), p.marketValue(), p.unrealizedPnl(),
                p.unrealizedPnlPercent(), p.realizedPnl());
    }
}