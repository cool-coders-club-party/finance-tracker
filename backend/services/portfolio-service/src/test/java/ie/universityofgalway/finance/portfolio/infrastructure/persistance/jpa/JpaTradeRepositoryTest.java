package ie.universityofgalway.finance.portfolio.infrastructure.persistance.jpa;

import ie.universityofgalway.finance.portfolio.domain.model.TradeType;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.HoldingEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.PortfolioEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.TradeEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa.JpaHoldingRepository;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa.JpaPortfolioRepository;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa.JpaTradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
class JpaTradeRepositoryTest {

    @Autowired private JpaPortfolioRepository portfolioRepository;
    @Autowired private JpaHoldingRepository holdingRepository;
    @Autowired private JpaTradeRepository tradeRepository;

    private PortfolioEntity portfolio(String userId, String name) {
        return portfolioRepository.save(new PortfolioEntity(userId, name));
    }

    // quantity and avg price are not-null columns that the entity constructor
    // doesn't set, so the helper does.
    private HoldingEntity holding(PortfolioEntity portfolio, String ticker) {
        HoldingEntity holding = new HoldingEntity(portfolio, ticker);
        holding.setQuantity(BigDecimal.ZERO);
        holding.setAvgPurchasePrice(BigDecimal.ZERO);
        return holdingRepository.save(holding);
    }

    private TradeEntity trade(HoldingEntity holding, TradeType type,
                              String quantity, String price, String when) {
        return tradeRepository.save(new TradeEntity(
                holding, type, new BigDecimal(quantity), new BigDecimal(price), Instant.parse(when)));
    }

    @Test
    void returnsTradesAcrossAllHoldingsInThePortfolioOldestFirst() {
        PortfolioEntity portfolio = portfolio("user-1", "Main");
        HoldingEntity aapl = holding(portfolio, "AAPL");
        HoldingEntity msft = holding(portfolio, "MSFT");

        // Deliberately saved out of chronological order, across two holdings.
        trade(msft, TradeType.BUY, "20", "415.20", "2026-10-02T10:00:00Z");
        trade(aapl, TradeType.SELL, "4", "200.00", "2026-10-03T10:00:00Z");
        trade(aapl, TradeType.BUY, "10", "192.45", "2026-10-01T10:00:00Z");

        assertThat(tradeRepository.findAllByPortfolioId(portfolio.getId()))
                .extracting(t -> t.getHolding().getTicker(), TradeEntity::getType)
                .containsExactly(
                        tuple("AAPL", TradeType.BUY),
                        tuple("MSFT", TradeType.BUY),
                        tuple("AAPL", TradeType.SELL));
    }

    @Test
    void tradesWithTheSameTimestampComeBackInTheOrderTheyWereSaved() {
        PortfolioEntity portfolio = portfolio("user-1", "Main");
        HoldingEntity aapl = holding(portfolio, "AAPL");

        trade(aapl, TradeType.BUY, "1", "100", "2026-10-01T10:00:00Z");
        trade(aapl, TradeType.BUY, "2", "100", "2026-10-01T10:00:00Z");

        assertThat(tradeRepository.findAllByPortfolioId(portfolio.getId()))
                .extracting(TradeEntity::getQuantity)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1"), new BigDecimal("2"));
    }

    @Test
    void doesNotReturnAnotherPortfoliosTrades() {
        PortfolioEntity mine = portfolio("user-1", "Mine");
        PortfolioEntity theirs = portfolio("user-2", "Theirs");
        HoldingEntity myAapl = holding(mine, "AAPL");
        HoldingEntity theirAapl = holding(theirs, "AAPL");

        trade(myAapl, TradeType.BUY, "10", "192.45", "2026-10-01T10:00:00Z");
        trade(theirAapl, TradeType.BUY, "99", "192.45", "2026-10-01T11:00:00Z");

        assertThat(tradeRepository.findAllByPortfolioId(mine.getId()))
                .extracting(TradeEntity::getQuantity)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("10"));
    }

    @Test
    void returnsEmptyListWhenPortfolioHasNoTrades() {
        PortfolioEntity empty = portfolio("user-1", "Empty");

        assertThat(tradeRepository.findAllByPortfolioId(empty.getId())).isEmpty();
    }
}