package ie.universityofgalway.finance.portfolio.infrastructure.persistance.adapter;

import ie.universityofgalway.finance.portfolio.domain.model.Holding;
import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;
import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import ie.universityofgalway.finance.portfolio.domain.model.TradeType;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.adapter.HoldingRepositoryAdapter;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.adapter.PortfolioRepositoryAdapter;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.adapter.TradeRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@Import({PortfolioRepositoryAdapter.class, HoldingRepositoryAdapter.class, TradeRepositoryAdapter.class})
class RepositoryAdaptersTest {

    @Autowired private PortfolioRepositoryAdapter portfolios;
    @Autowired private HoldingRepositoryAdapter holdings;
    @Autowired private TradeRepositoryAdapter trades;

    @Test
    void savedPortfolioGetsAnIdAndIsOnlyVisibleToItsOwner() {
        Portfolio saved = portfolios.save(Portfolio.create("user-1", "Main"));

        assertThat(saved.getId()).isNotNull();
        assertThat(portfolios.findByIdAndUserId(saved.getId(), "user-1")).isPresent();
        assertThat(portfolios.findByIdAndUserId(saved.getId(), "user-2")).isEmpty();
    }

    @Test
    void savingAnExistingHoldingUpdatesItInsteadOfCreatingADuplicate() {
        Portfolio portfolio = portfolios.save(Portfolio.create("user-1", "Main"));

        Holding holding = holdings.save(Holding.open(portfolio.getId(), "AAPL"));
        assertThat(holding.getId()).isNotNull();

        holding.buy(new BigDecimal("10"), new BigDecimal("100"), Instant.now());
        holding.buy(new BigDecimal("10"), new BigDecimal("200"), Instant.now());
        Holding updated = holdings.save(holding);

        assertThat(updated.getId()).isEqualTo(holding.getId());
        assertThat(holdings.findByPortfolioId(portfolio.getId())).hasSize(1);
        assertThat(updated.getQuantity()).isEqualByComparingTo("20");
        assertThat(updated.getAvgPurchasePrice()).isEqualByComparingTo("150");
    }

    @Test
    void tradesComeBackOldestFirstWithTheirTickerAndPortfolioId() {
        Portfolio portfolio = portfolios.save(Portfolio.create("user-1", "Main"));

        Holding aapl = holdings.save(Holding.open(portfolio.getId(), "AAPL"));
        Trade buy = aapl.buy(new BigDecimal("10"), new BigDecimal("192.45"),
                Instant.parse("2026-10-01T10:00:00Z"));
        aapl = holdings.save(aapl);
        trades.save(buy);

        Trade sell = aapl.sell(new BigDecimal("4"), new BigDecimal("200"),
                Instant.parse("2026-10-02T10:00:00Z"));
        aapl = holdings.save(aapl);
        trades.save(sell);

        assertThat(trades.findAllByPortfolioId(portfolio.getId()))
                .extracting(Trade::getTicker, Trade::getType, Trade::getPortfolioId)
                .containsExactly(
                        tuple("AAPL", TradeType.BUY, portfolio.getId()),
                        tuple("AAPL", TradeType.SELL, portfolio.getId()));
    }

    @Test
    void aTradeForAHoldingThatWasNeverSavedIsRejected() {
        Portfolio portfolio = portfolios.save(Portfolio.create("user-1", "Main"));
        Trade orphan = new Trade(null, portfolio.getId(), "AAPL", TradeType.BUY,
                new BigDecimal("1"), new BigDecimal("100"), Instant.now());

        assertThatThrownBy(() -> trades.save(orphan)).isInstanceOf(IllegalStateException.class);
    }
}