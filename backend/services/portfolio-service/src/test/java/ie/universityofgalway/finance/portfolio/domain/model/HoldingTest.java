package ie.universityofgalway.finance.portfolio.domain.model;

import ie.universityofgalway.finance.portfolio.domain.exception.InsufficientSharesException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoldingTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void firstBuySetsQuantityAndAveragePrice() {
        Holding holding = Holding.open(1L, "AAPL");

        holding.buy(bd("10"), bd("192.45"), NOW);

        assertThat(holding.getQuantity()).isEqualByComparingTo("10");
        assertThat(holding.getAvgPurchasePrice()).isEqualByComparingTo("192.45");
    }

    @Test
    void secondBuyAtDifferentPriceRecalculatesWeightedAverage() {
        Holding holding = Holding.open(1L, "AAPL");
        holding.buy(bd("10"), bd("100"), NOW);

        // 10 @ 100 + 10 @ 200 = 3000 total value / 20 shares = 150
        holding.buy(bd("10"), bd("200"), NOW);

        assertThat(holding.getQuantity()).isEqualByComparingTo("20");
        assertThat(holding.getAvgPurchasePrice()).isEqualByComparingTo("150");
    }

    @Test
    void buyReturnsTheTradeItRecorded() {
        Holding holding = Holding.open(7L, "MSFT");

        Trade trade = holding.buy(bd("3"), bd("415.20"), NOW);

        assertThat(trade.getType()).isEqualTo(TradeType.BUY);
        assertThat(trade.getPortfolioId()).isEqualTo(7L);
        assertThat(trade.getTicker()).isEqualTo("MSFT");
        assertThat(trade.getQuantity()).isEqualByComparingTo("3");
        assertThat(trade.getPrice()).isEqualByComparingTo("415.20");
        assertThat(trade.getExecutedAt()).isEqualTo(NOW);
    }

    @Test
    void sellReducesQuantityButLeavesAveragePriceUnchanged() {
        Holding holding = new Holding(1L, 1L, "AAPL", bd("10"), bd("150.00"));

        Trade trade = holding.sell(bd("4"), bd("200"), NOW);

        assertThat(holding.getQuantity()).isEqualByComparingTo("6");
        assertThat(holding.getAvgPurchasePrice()).isEqualByComparingTo("150.00");
        assertThat(trade.getType()).isEqualTo(TradeType.SELL);
        assertThat(trade.getPrice()).isEqualByComparingTo("200");
    }

    @Test
    void sellingEverythingLeavesZeroQuantity() {
        Holding holding = new Holding(1L, 1L, "AAPL", bd("10"), bd("150.00"));

        holding.sell(bd("10"), bd("200"), NOW);

        assertThat(holding.getQuantity()).isEqualByComparingTo("0");
    }

    @Test
    void sellingMoreThanOwnedThrowsAndLeavesHoldingUnchanged() {
        Holding holding = new Holding(1L, 1L, "AAPL", bd("5"), bd("100.00"));

        assertThatThrownBy(() -> holding.sell(bd("10"), bd("200"), NOW))
                .isInstanceOf(InsufficientSharesException.class);

        assertThat(holding.getQuantity()).isEqualByComparingTo("5");
    }

    @Test
    void zeroOrNegativeQuantityIsRejectedOnBuyAndSell() {
        Holding holding = new Holding(1L, 1L, "AAPL", bd("5"), bd("100.00"));

        assertThatThrownBy(() -> holding.buy(BigDecimal.ZERO, bd("100"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> holding.buy(bd("-1"), bd("100"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> holding.sell(BigDecimal.ZERO, bd("100"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
