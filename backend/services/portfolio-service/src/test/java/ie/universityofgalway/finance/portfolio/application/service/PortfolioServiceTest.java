package ie.universityofgalway.finance.portfolio.application.service;

import ie.universityofgalway.finance.portfolio.application.port.HoldingRepository;
import ie.universityofgalway.finance.portfolio.application.port.PortfolioRepository;
import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.application.port.TradeRepository;
import ie.universityofgalway.finance.portfolio.domain.exception.InsufficientSharesException;
import ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException;
import ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException;
import ie.universityofgalway.finance.portfolio.domain.model.Holding;
import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;
import ie.universityofgalway.finance.portfolio.domain.model.Trade;
import ie.universityofgalway.finance.portfolio.domain.model.TradeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock private PortfolioRepository portfolioRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private TradeRepository tradeRepository;
    @Mock private PriceClient priceClient;

    @Captor private ArgumentCaptor<Trade> tradeCaptor;

    @InjectMocks
    private PortfolioService portfolioService;

    private final Portfolio portfolio = new Portfolio(1L, "user-1", "Main");

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private void callerOwnsPortfolio() {
        when(portfolioRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(portfolio));
    }

    // ---------- buy ----------

    @Test
    void buyCreatesHoldingWhenNoneExistsAndSavesHoldingBeforeItsTrade() {
        callerOwnsPortfolio();
        when(priceClient.getPrice("AAPL")).thenReturn(bd("192.45"));
        when(holdingRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.empty());
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

        Holding result = portfolioService.buy(1L, "user-1", "aapl", bd("10"));

        assertThat(result.getTicker()).isEqualTo("AAPL");
        assertThat(result.getQuantity()).isEqualByComparingTo("10");
        assertThat(result.getAvgPurchasePrice()).isEqualByComparingTo("192.45");

        // The adapter looks the holding up by portfolio + ticker when saving a trade,
        // so the holding must be persisted first.
        InOrder order = inOrder(holdingRepository, tradeRepository);
        order.verify(holdingRepository).save(any(Holding.class));
        order.verify(tradeRepository).save(tradeCaptor.capture());

        Trade trade = tradeCaptor.getValue();
        assertThat(trade.getType()).isEqualTo(TradeType.BUY);
        assertThat(trade.getTicker()).isEqualTo("AAPL");
        assertThat(trade.getQuantity()).isEqualByComparingTo("10");
        assertThat(trade.getPrice()).isEqualByComparingTo("192.45");
    }

    @Test
    void buyOnAnExistingHoldingBlendsTheAveragePrice() {
        Holding existing = new Holding(5L, 1L, "AAPL", bd("10"), bd("100.00"));
        callerOwnsPortfolio();
        when(priceClient.getPrice("AAPL")).thenReturn(bd("200.00"));
        when(holdingRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.of(existing));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

        Holding result = portfolioService.buy(1L, "user-1", "AAPL", bd("10"));

        assertThat(result.getQuantity()).isEqualByComparingTo("20");
        assertThat(result.getAvgPurchasePrice()).isEqualByComparingTo("150");
    }

    @Test
    void buyWithAnUnknownTickerSavesNothing() {
        callerOwnsPortfolio();
        when(priceClient.getPrice("ZZZZ")).thenThrow(new InvalidSymbolException("ZZZZ"));

        assertThatThrownBy(() -> portfolioService.buy(1L, "user-1", "ZZZZ", BigDecimal.ONE))
                .isInstanceOf(InvalidSymbolException.class);

        verifyNoInteractions(holdingRepository, tradeRepository);
    }

    @Test
    void buyOnSomeoneElsesPortfolioIsNotFoundAndTouchesNothing() {
        when(portfolioRepository.findByIdAndUserId(1L, "other-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.buy(1L, "other-user", "AAPL", BigDecimal.ONE))
                .isInstanceOf(PortfolioNotFoundException.class);

        verifyNoInteractions(priceClient, holdingRepository, tradeRepository);
    }

    // ---------- sell ----------

    @Test
    void sellReducesTheHoldingAndRecordsASellTrade() {
        Holding existing = new Holding(5L, 1L, "AAPL", bd("10"), bd("150.00"));
        callerOwnsPortfolio();
        when(holdingRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.of(existing));
        when(priceClient.getPrice("AAPL")).thenReturn(bd("200.00"));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

        Holding result = portfolioService.sell(1L, "user-1", "AAPL", bd("4"));

        assertThat(result.getQuantity()).isEqualByComparingTo("6");
        assertThat(result.getAvgPurchasePrice()).isEqualByComparingTo("150.00");

        verify(tradeRepository).save(tradeCaptor.capture());
        assertThat(tradeCaptor.getValue().getType()).isEqualTo(TradeType.SELL);
        assertThat(tradeCaptor.getValue().getPrice()).isEqualByComparingTo("200.00");
    }

    @Test
    void sellingMoreThanOwnedThrowsAndSavesNothing() {
        Holding existing = new Holding(5L, 1L, "AAPL", bd("5"), bd("100.00"));
        callerOwnsPortfolio();
        when(holdingRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.of(existing));
        when(priceClient.getPrice("AAPL")).thenReturn(bd("200.00"));

        assertThatThrownBy(() -> portfolioService.sell(1L, "user-1", "AAPL", bd("10")))
                .isInstanceOf(InsufficientSharesException.class);

        verify(holdingRepository, never()).save(any(Holding.class));
        verifyNoInteractions(tradeRepository);
    }

    @Test
    void sellingATickerYouDoNotHoldIsTreatedAsInsufficientShares() {
        callerOwnsPortfolio();
        when(holdingRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.sell(1L, "user-1", "AAPL", bd("1")))
                .isInstanceOf(InsufficientSharesException.class);

        verifyNoInteractions(tradeRepository);
    }

    // ---------- reads ----------

    @Test
    void getPortfoliosReturnsTheUsersPortfolios() {
        Portfolio second = new Portfolio(2L, "user-1", "Retirement");
        when(portfolioRepository.findByUserId("user-1")).thenReturn(List.of(portfolio, second));

        assertThat(portfolioService.getPortfolios("user-1")).containsExactly(portfolio, second);
    }

    @Test
    void getPortfoliosReturnsAnEmptyListForAUserWithNone() {
        when(portfolioRepository.findByUserId("new-user")).thenReturn(List.of());

        assertThat(portfolioService.getPortfolios("new-user")).isEmpty();
    }

    @Test
    void getHoldingsReturnsHoldingsForAnOwnedPortfolio() {
        Holding aapl = Holding.open(1L, "AAPL");
        Holding msft = Holding.open(1L, "MSFT");
        callerOwnsPortfolio();
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(aapl, msft));

        assertThat(portfolioService.getHoldings(1L, "user-1")).containsExactly(aapl, msft);
    }

    @Test
    void getHoldingsForSomeoneElsesPortfolioIsNotFoundAndNeverQueriesHoldings() {
        when(portfolioRepository.findByIdAndUserId(1L, "other-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getHoldings(1L, "other-user"))
                .isInstanceOf(PortfolioNotFoundException.class);

        verifyNoInteractions(holdingRepository);
    }

    @Test
    void getTradesReturnsTradesForAnOwnedPortfolio() {
        Trade buy = new Trade(1L, 1L, "AAPL", TradeType.BUY, bd("10"), bd("192.45"), Instant.now());
        Trade sell = new Trade(2L, 1L, "AAPL", TradeType.SELL, bd("4"), bd("200.00"), Instant.now());
        callerOwnsPortfolio();
        when(tradeRepository.findAllByPortfolioId(1L)).thenReturn(List.of(buy, sell));

        assertThat(portfolioService.getTrades(1L, "user-1")).containsExactly(buy, sell);
    }

    @Test
    void getTradesForSomeoneElsesPortfolioIsNotFoundAndNeverQueriesTrades() {
        when(portfolioRepository.findByIdAndUserId(1L, "other-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getTrades(1L, "other-user"))
                .isInstanceOf(PortfolioNotFoundException.class);

        verifyNoInteractions(tradeRepository);
    }
}