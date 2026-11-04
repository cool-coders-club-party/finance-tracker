package ie.universityofgalway.finance.portfolio.domain.service;

import ie.universityofgalway.finance.portfolio.domain.model.HoldingPerformance;
import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import ie.universityofgalway.finance.portfolio.domain.model.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerformanceCalculatorTest {

    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");

    private long counter;

    @BeforeEach
    void resetCounter() {
        counter = 0;
    }

    // Each call creates a trade one minute after the previous one, so the list
    // order in a test matches "oldest first", just like the repository returns it.
    private Trade trade(TradeType type, String qty, String price) {
        counter++;
        return new Trade(
                counter,                       // id
                1L,                            // portfolioId
                "AAPL",                        // ticker
                type,
                new BigDecimal(qty),
                new BigDecimal(price),
                START.plusSeconds(counter * 60)  // executedAt
        );
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void unrealizedProfitOnASingleBuy() {
        HoldingPerformance p = PerformanceCalculator.calculate("AAPL",
                List.of(trade(TradeType.BUY, "10", "192.45")), bd("200"));

        assertThat(p.costBasis()).isEqualByComparingTo("1924.50");
        assertThat(p.marketValue()).isEqualByComparingTo("2000.00");
        assertThat(p.unrealizedPnl()).isEqualByComparingTo("75.50");
        assertThat(p.unrealizedPnlPercent()).isEqualByComparingTo("3.92");
        assertThat(p.realizedPnl()).isEqualByComparingTo("0.00");
    }

    @Test
    void unrealizedLossIsNegative() {
        HoldingPerformance p = PerformanceCalculator.calculate("AAPL",
                List.of(trade(TradeType.BUY, "10", "100")), bd("80"));

        assertThat(p.unrealizedPnl()).isEqualByComparingTo("-200.00");
        assertThat(p.unrealizedPnlPercent()).isEqualByComparingTo("-20.00");
    }

    @Test
    void partialSaleRealizesAgainstWeightedAverage() {
        HoldingPerformance p = PerformanceCalculator.calculate("AAPL", List.of(
                trade(TradeType.BUY, "10", "100"),
                trade(TradeType.BUY, "10", "200"),
                trade(TradeType.SELL, "5", "300")), bd("180"));

        assertThat(p.realizedPnl()).isEqualByComparingTo("750.00");
        assertThat(p.unrealizedPnl()).isEqualByComparingTo("450.00");
        assertThat(p.unrealizedPnlPercent()).isEqualByComparingTo("20.00");
    }

    @Test
    void averageAtTimeOfSaleIsUsedNotTheFinalAverage() {
        HoldingPerformance p = PerformanceCalculator.calculate("AAPL", List.of(
                        trade(TradeType.BUY, "10", "100"),
                        trade(TradeType.SELL, "5", "120"),   // +100 at avg 100
                        trade(TradeType.BUY, "5", "200"),    // avg becomes 150
                        trade(TradeType.SELL, "5", "180")),  // +150 at avg 150
                bd("180"));

        assertThat(p.realizedPnl()).isEqualByComparingTo("250.00");
    }

    @Test
    void closedPositionNeedsNoPrice() {
        HoldingPerformance p = PerformanceCalculator.calculate("AAPL", List.of(
                trade(TradeType.BUY, "10", "100"),
                trade(TradeType.SELL, "10", "120")), null);

        assertThat(p.realizedPnl()).isEqualByComparingTo("200.00");
        assertThat(p.unrealizedPnl()).isEqualByComparingTo("0.00");
        assertThat(p.currentPrice()).isNull();
    }

    @Test
    void openPositionWithoutPriceIsRejected() {
        assertThatThrownBy(() -> PerformanceCalculator.calculate("AAPL",
                List.of(trade(TradeType.BUY, "10", "100")), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}