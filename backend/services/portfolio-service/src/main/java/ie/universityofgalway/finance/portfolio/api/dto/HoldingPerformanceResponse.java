package ie.universityofgalway.finance.portfolio.api.dto;

import ie.universityofgalway.finance.portfolio.domain.model.HoldingPerformance;

import java.math.BigDecimal;

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
    public static HoldingPerformanceResponse from(HoldingPerformance p) {
        return new HoldingPerformanceResponse(
                p.ticker(), p.quantity(), p.avgPurchasePrice(), p.currentPrice(),
                p.costBasis(), p.marketValue(), p.unrealizedPnl(),
                p.unrealizedPnlPercent(), p.realizedPnl());
    }
}