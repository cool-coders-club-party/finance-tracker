package ie.universityofgalway.finance.portfolio.application.service;

import ie.universityofgalway.finance.portfolio.application.port.PortfolioRepository;
import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.application.port.TradeRepository;
import ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException;
import ie.universityofgalway.finance.portfolio.domain.exception.PriceUnavailableException;
import ie.universityofgalway.finance.portfolio.domain.model.HoldingPerformance;
import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;
import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import ie.universityofgalway.finance.portfolio.domain.model.TradeType;
import ie.universityofgalway.finance.portfolio.domain.service.PerformanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PerformanceServiceTest {

    private static final Long PORTFOLIO_ID = 1L;
    private static final String USER = "user-1";

    private PortfolioRepository portfolioRepository;
    private TradeRepository tradeRepository;
    private FakePriceClient priceClient;
    private PerformanceService service;

    private long counter;

    // Hand-written fake: returns fixed prices and records which tickers were asked for.
    private static class FakePriceClient implements PriceClient {
        final Map<String, BigDecimal> prices;
        final List<String> requested = new ArrayList<>();
        RuntimeException failure;

        FakePriceClient(Map<String, BigDecimal> prices) {
            this.prices = prices;
        }

        @Override
        public BigDecimal getPrice(String ticker) {
            requested.add(ticker);
            if (failure != null) {
                throw failure;
            }
            return prices.get(ticker);
        }
    }

    @BeforeEach
    void setUp() {
        counter = 0;
        portfolioRepository = mock(PortfolioRepository.class);
        tradeRepository = mock(TradeRepository.class);
        priceClient = new FakePriceClient(Map.of(
                "AAPL", new BigDecimal("200.00"),
                "MSFT", new BigDecimal("400.00")));
        service = new PerformanceService(portfolioRepository, tradeRepository, priceClient);
    }

    // The service only checks that a portfolio exists, so a mock is enough here.
    private void ownedPortfolioExists() {
        when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER))
                .thenReturn(Optional.of(mock(Portfolio.class)));
    }

    // Counter gives each trade a unique id and an increasing timestamp (oldest first).
    private Trade trade(String ticker, TradeType type, String qty, String price) {
        counter++;
        return new Trade(counter, PORTFOLIO_ID, ticker, type,
                new BigDecimal(qty), new BigDecimal(price),
                Instant.parse("2026-01-01T00:00:00Z").plusSeconds(counter));
    }

    @Test
    void otherUsersPortfolioLooksLikeNotFound() {
        when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, "intruder"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPerformance(PORTFOLIO_ID, "intruder"))
                .isInstanceOf(PortfolioNotFoundException.class);

        verifyNoInteractions(tradeRepository);
        assertThat(priceClient.requested).isEmpty();
    }

    @Test
    void openPositionIsPricedAndCalculated() {
        ownedPortfolioExists();
        List<Trade> trades = List.of(trade("AAPL", TradeType.BUY, "10", "100"));
        when(tradeRepository.findAllByPortfolioId(PORTFOLIO_ID)).thenReturn(trades);

        List<HoldingPerformance> result = service.getPerformance(PORTFOLIO_ID, USER);

        assertThat(priceClient.requested).containsExactly("AAPL");
        assertThat(result).containsExactly(
                PerformanceCalculator.calculate("AAPL", trades, new BigDecimal("200.00")));
    }

    @Test
    void closedPositionIsNotPriced() {
        ownedPortfolioExists();
        List<Trade> trades = List.of(
                trade("AAPL", TradeType.BUY, "10", "100"),
                trade("AAPL", TradeType.SELL, "10", "120"));
        when(tradeRepository.findAllByPortfolioId(PORTFOLIO_ID)).thenReturn(trades);

        List<HoldingPerformance> result = service.getPerformance(PORTFOLIO_ID, USER);

        assertThat(priceClient.requested).isEmpty();
        assertThat(result).containsExactly(PerformanceCalculator.calculate("AAPL", trades, null));
    }

    @Test
    void resultsAreSortedByTickerAndEachTickerGetsOnlyItsOwnTrades() {
        ownedPortfolioExists();
        // Deliberately interleaved and with MSFT first, to prove grouping and sorting.
        Trade msft1 = trade("MSFT", TradeType.BUY, "5", "300");
        Trade aapl1 = trade("AAPL", TradeType.BUY, "10", "100");
        Trade msft2 = trade("MSFT", TradeType.BUY, "5", "350");
        Trade aapl2 = trade("AAPL", TradeType.SELL, "4", "150");
        when(tradeRepository.findAllByPortfolioId(PORTFOLIO_ID))
                .thenReturn(List.of(msft1, aapl1, msft2, aapl2));

        List<HoldingPerformance> result = service.getPerformance(PORTFOLIO_ID, USER);

        assertThat(result).containsExactly(
                PerformanceCalculator.calculate("AAPL", List.of(aapl1, aapl2), new BigDecimal("200.00")),
                PerformanceCalculator.calculate("MSFT", List.of(msft1, msft2), new BigDecimal("400.00")));
        assertThat(priceClient.requested).containsExactly("AAPL", "MSFT");
    }

    @Test
    void noTradesGivesEmptyList() {
        ownedPortfolioExists();
        when(tradeRepository.findAllByPortfolioId(PORTFOLIO_ID)).thenReturn(List.of());

        assertThat(service.getPerformance(PORTFOLIO_ID, USER)).isEmpty();
        assertThat(priceClient.requested).isEmpty();
    }

    @Test
    void priceFailurePropagatesSoTheApiCanReturn503() {
        ownedPortfolioExists();
        when(tradeRepository.findAllByPortfolioId(PORTFOLIO_ID))
                .thenReturn(List.of(trade("AAPL", TradeType.BUY, "10", "100")));
        priceClient.failure = new PriceUnavailableException("Market Data down", null);

        assertThatThrownBy(() -> service.getPerformance(PORTFOLIO_ID, USER))
                .isInstanceOf(PriceUnavailableException.class);
    }
}