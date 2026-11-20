package ie.universityofgalway.finance.portfolio.application.service;

import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.domain.exception.*;
import ie.universityofgalway.finance.portfolio.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies exact valuation arithmetic, stale metadata, currency restrictions, incomplete totals and
 * authorisation before pricing.
 */
class ValuationServiceTest {
    private final PortfolioService portfolios = mock(PortfolioService.class);
    private final PriceClient prices = mock(PriceClient.class);
    private final ValuationService service = new ValuationService(portfolios, prices);
    private final Instant asOf = Instant.parse("2026-11-16T12:00:00Z");

    private Holding holding(String ticker, String currency) {
        return new Holding(1L, 5L, ticker, new BigDecimal("2.5"), new BigDecimal("100"), currency);
    }
    private MarketQuote quote(String ticker, String price, String currency, boolean stale) {
        return new MarketQuote(ticker, new BigDecimal(price), currency, asOf, stale);
    }
    @BeforeEach void holdings() {
        when(portfolios.getHoldings(5L, "alice")).thenReturn(List.of(holding("AAPL", "USD")));
    }
    @Test void calculatesFractionalHoldingsAndPreservesStaleQuoteWithoutChangingSavedCost() {
        when(prices.getQuote("AAPL")).thenReturn(quote("AAPL", "120.25", "USD", true));
        PortfolioValuation result = service.getValuation(5L, "alice");
        assertThat(result.complete()).isTrue();
        assertThat(result.stale()).isTrue();
        assertThat(result.costBasis()).isEqualByComparingTo("250");
        assertThat(result.currentValue()).isEqualByComparingTo("300.625");
        assertThat(result.unrealisedGainLoss()).isEqualByComparingTo("50.625");
        assertThat(result.holdings().getFirst().quote().asOf()).isEqualTo(asOf);
        assertThat(result.holdings().getFirst().holding().getAvgPurchasePrice()).isEqualByComparingTo("100");
        verify(portfolios).getHoldings(5L, "alice");
        verifyNoMoreInteractions(portfolios);
    }
    @Test void calculatesLossAndCompleteTotalsAcrossHoldings() {
        when(portfolios.getHoldings(5L, "alice")).thenReturn(List.of(holding("AAPL", "USD"), holding("MSFT", "USD")));
        when(prices.getQuote("AAPL")).thenReturn(quote("AAPL", "90", "USD", false));
        when(prices.getQuote("MSFT")).thenReturn(quote("MSFT", "95", "USD", false));
        var result = service.getValuation(5L, "alice");
        assertThat(result.currentValue()).isEqualByComparingTo("462.5");
        assertThat(result.unrealisedGainLoss()).isEqualByComparingTo("-37.5");
        assertThat(result.stale()).isFalse();
    }
    @Test void failedQuoteKeepsEveryHoldingButWithholdsTotals() {
        when(portfolios.getHoldings(5L, "alice")).thenReturn(List.of(holding("AAPL", "USD"), holding("MSFT", "USD")));
        when(prices.getQuote("AAPL")).thenThrow(new PriceUnavailableException("offline", null));
        when(prices.getQuote("MSFT")).thenReturn(quote("MSFT", "120", "USD", false));
        var result = service.getValuation(5L, "alice");
        assertThat(result.holdings()).hasSize(2);
        assertThat(result.holdings().get(1).available()).isTrue();
        assertThat(result.unavailableHoldings()).isEqualTo(1);
        assertThat(result.complete()).isFalse();
        assertThat(result.currentValue()).isNull();
        assertThat(result.costBasis()).isNull();
        assertThat(result.unrealisedGainLoss()).isNull();
        assertThat(result.holdings().getFirst().unavailableReason()).isEqualTo("QUOTE_UNAVAILABLE");
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"EUR", "GBP"})
    void missingOrIncompatibleQuoteCurrencyIsUnavailable(String currency) {
        when(prices.getQuote("AAPL")).thenReturn(quote("AAPL", "120", currency, false));
        var result = service.getValuation(5L, "alice");
        assertThat(result.complete()).isFalse();
        assertThat(result.holdings().getFirst().currentValue()).isNull();
        assertThat(result.holdings().getFirst().quote().currency()).isEqualTo(currency);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"EUR"})
    void neverInfersPurchaseCurrencyFromQuote(String currency) {
        when(portfolios.getHoldings(5L, "alice")).thenReturn(List.of(holding("AAPL", currency)));
        when(prices.getQuote("AAPL")).thenReturn(quote("AAPL", "120", "USD", false));
        assertThat(service.getValuation(5L, "alice").complete()).isFalse();
    }
    @Test void invalidQuoteIsUnavailable() {
        when(prices.getQuote("AAPL")).thenReturn(quote("MSFT", "120", "USD", false));
        assertThat(service.getValuation(5L, "alice").holdings().getFirst().unavailableReason()).isEqualTo("INVALID_QUOTE");
    }
    @Test void unknownSymbolIsUnavailable() {
        when(prices.getQuote("AAPL")).thenThrow(new InvalidSymbolException("AAPL"));
        assertThat(service.getValuation(5L, "alice").complete()).isFalse();
    }
    @Test void emptyPortfolioHasZeroCompleteTotalsAndMakesNoQuoteRequests() {
        when(portfolios.getHoldings(5L, "alice")).thenReturn(List.of());
        var result = service.getValuation(5L, "alice");
        assertThat(result.complete()).isTrue();
        assertThat(result.currentValue()).isEqualByComparingTo("0");
        verifyNoInteractions(prices);
    }
    @Test void ownershipFailurePreventsQuoteRequests() {
        when(portfolios.getHoldings(5L, "alice")).thenThrow(new PortfolioNotFoundException(5L));
        assertThatThrownBy(() -> service.getValuation(5L, "alice")).isInstanceOf(PortfolioNotFoundException.class);
        verifyNoInteractions(prices);
    }
}
