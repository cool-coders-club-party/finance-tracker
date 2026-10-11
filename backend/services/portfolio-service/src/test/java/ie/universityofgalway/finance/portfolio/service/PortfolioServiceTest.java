package ie.universityofgalway.finance.portfolio.service;

import ie.universityofgalway.finance.portfolio.exception.InsufficientSharesException;
import ie.universityofgalway.finance.portfolio.exception.InvalidSymbolException;
import ie.universityofgalway.finance.portfolio.model.Holding;
import ie.universityofgalway.finance.portfolio.model.Portfolio;
import ie.universityofgalway.finance.portfolio.price.PriceClient;
import ie.universityofgalway.finance.portfolio.repository.HoldingRepository;
import ie.universityofgalway.finance.portfolio.repository.PortfolioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock private PortfolioRepository portfolioRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private PriceClient priceClient;

    @InjectMocks
    private PortfolioService portfolioService;

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio("user-1", "My Portfolio");
    }

    @Test
    void buyingNewTickerCreatesHoldingAtPurchasePrice() {
        when(portfolioRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(portfolio));
        when(priceClient.getPrice("AAPL")).thenReturn(new BigDecimal("192.45"));
        when(holdingRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.empty());
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

        Holding result = portfolioService.buy(1L, "user-1", "AAPL", new BigDecimal("10"));

        assertThat(result.getQuantity()).isEqualByComparingTo("10");
        assertThat(result.getAvgPurchasePrice()).isEqualByComparingTo("192.45");
        assertThat(result.getTrades()).hasSize(1);
    }

    @Test
    void secondBuyAtDifferentPriceRecalculatesWeightedAverage() {
        Holding existing = new Holding(portfolio, "AAPL");
        existing.setQuantity(new BigDecimal("10"));
        existing.setAvgPurchasePrice(new BigDecimal("100.00"));

        when(portfolioRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(portfolio));
        when(priceClient.getPrice("AAPL")).thenReturn(new BigDecimal("200.00"));
        when(holdingRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.of(existing));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

        // 10 @ 100 + 10 @ 200 = 3000 total value / 20 shares = 150 avg
        Holding result = portfolioService.buy(1L, "user-1", "AAPL", new BigDecimal("10"));

        assertThat(result.getQuantity()).isEqualByComparingTo("20");
        assertThat(result.getAvgPurchasePrice()).isEqualByComparingTo("150.0000");
    }

    @Test
    void buyingUnknownTickerThrowsInvalidSymbolException() {
        when(portfolioRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(portfolio));
        when(priceClient.getPrice("ZZZZ")).thenThrow(new InvalidSymbolException("ZZZZ"));

        assertThatThrownBy(() -> portfolioService.buy(1L, "user-1", "ZZZZ", BigDecimal.ONE))
                .isInstanceOf(InvalidSymbolException.class);
    }

    @Test
    void sellingMoreThanOwnedThrowsInsufficientShares() {
        Holding existing = new Holding(portfolio, "AAPL");
        existing.setQuantity(new BigDecimal("5"));
        existing.setAvgPurchasePrice(new BigDecimal("100.00"));

        when(portfolioRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> portfolioService.sell(1L, "user-1", "AAPL", new BigDecimal("10")))
                .isInstanceOf(InsufficientSharesException.class);
    }

    @Test
    void sellingReducesQuantityButLeavesAvgPriceUnchanged() {
        Holding existing = new Holding(portfolio, "AAPL");
        existing.setQuantity(new BigDecimal("10"));
        existing.setAvgPurchasePrice(new BigDecimal("150.00"));

        when(portfolioRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioIdAndTicker(1L, "AAPL")).thenReturn(Optional.of(existing));
        when(priceClient.getPrice("AAPL")).thenReturn(new BigDecimal("200.00"));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

        Holding result = portfolioService.sell(1L, "user-1", "AAPL", new BigDecimal("4"));

        assertThat(result.getQuantity()).isEqualByComparingTo("6");
        assertThat(result.getAvgPurchasePrice()).isEqualByComparingTo("150.00"); // unchanged
        assertThat(result.getTrades()).hasSize(1);
    }

    @Test
    void zeroOrNegativeQuantityIsRejected() {
        assertThatThrownBy(() -> portfolioService.buy(1L, "user-1", "AAPL", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getHoldingsReturnsHoldingsForOwnedPortfolio() {
        Holding aapl = new Holding(portfolio, "AAPL");
        Holding msft = new Holding(portfolio, "MSFT");

        when(portfolioRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(aapl, msft));

        List<Holding> result = portfolioService.getHoldings(1L, "user-1");

        assertThat(result).containsExactly(aapl, msft);
    }

    @Test
    void getHoldingsReturnsEmptyListForPortfolioWithNoHoldings() {
        when(portfolioRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of());

        assertThat(portfolioService.getHoldings(1L, "user-1")).isEmpty();
    }

    @Test
    void getHoldingsThrowsNotFoundWhenPortfolioBelongsToSomeoneElse() {
        // findByIdAndUserId returns empty when the portfolio exists but the
        // userId doesn't match - same result as "doesn't exist" by design.
        when(portfolioRepository.findByIdAndUserId(1L, "other-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getHoldings(1L, "other-user"))
                .isInstanceOf(EntityNotFoundException.class);

        // Holdings must never even be queried if the ownership check fails.
        verifyNoInteractions(holdingRepository);
    }
}
