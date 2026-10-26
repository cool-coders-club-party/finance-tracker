package ie.universityofgalway.finance.portfolio.repository;

import ie.universityofgalway.finance.portfolio.model.Holding;
import ie.universityofgalway.finance.portfolio.model.Portfolio;
import ie.universityofgalway.finance.portfolio.model.Trade;
import ie.universityofgalway.finance.portfolio.model.TradeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
class TradeRepositoryTest {

    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private HoldingRepository holdingRepository;
    @Autowired private TradeRepository tradeRepository;

    private Trade trade(Holding holding, TradeType type, String quantity, String price, String when) {
        return tradeRepository.save(new Trade(
                holding, type, new BigDecimal(quantity), new BigDecimal(price), Instant.parse(when)));
    }

    @Test
    void returnsTradesAcrossAllHoldingsInThePortfolioOldestFirst(){
        Portfolio portfolio = portfolioRepository.save(new Portfolio("user-1", "Mian"));
        Holding aapl = holdingRepository.save(new Holding(portfolio, "AAPL"));
        Holding msft = holdingRepository.save(new Holding(portfolio, "MSFT"));

        trade(msft, TradeType.BUY, "20", "415.20", "2026-10-02T10:00:00Z");
        trade(aapl, TradeType.SELL, "4", "200.00", "2026-10-03T10:00:00Z");
        trade(aapl, TradeType.BUY, "10", "192.45", "2026-10-01T10:00:00Z");

        assertThat(tradeRepository.findAllByPortfolioId(portfolio.getId()))
                .extracting(t -> t.getHolding().getTicker(), Trade::getType)
                .containsExactly(
                        tuple("AAPL", TradeType.BUY),
                        tuple("MSFT", TradeType.BUY),
                        tuple("AAPL", TradeType.SELL));
    }

    @Test
    void doesNotReturnAnotherPortfoliosTrades(){
        Portfolio mine = portfolioRepository.save(new Portfolio("user-1", "Main"));
        Portfolio theirs = portfolioRepository.save(new Portfolio("user-2", "Mian"));
        Holding myAAPL = holdingRepository.save(new Holding(mine, "AAPL"));
        Holding theirAAPL = holdingRepository.save(new Holding(theirs, "AAPL"));

        trade(myAAPL, TradeType.BUY, "10", "192.45", "2026-10-01T10:00:00Z");
        trade(theirAAPL, TradeType.BUY, "99", "192.45", "2026-10-01T11:00:00Z");

        assertThat(tradeRepository.findAllByPortfolioId(mine.getId()))
            .extracting(Trade::getQuantity)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("10"));
    }

    @Test
    void returnsEmptyListWhenPortfolioIsEmpty(){
        Portfolio portfolio = portfolioRepository.save(new Portfolio("user-1", "Main"));

        assertThat(tradeRepository.findAllByPortfolioId(portfolio.getId())).isEmpty();
    }

}
