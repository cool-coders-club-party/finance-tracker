package ie.universityofgalway.finance.portfolio.api.controller;

import ie.universityofgalway.finance.portfolio.application.port.PriceClient;
import ie.universityofgalway.finance.portfolio.application.service.PortfolioService;
import ie.universityofgalway.finance.portfolio.application.service.ValuationService;
import ie.universityofgalway.finance.portfolio.domain.exception.*;
import ie.universityofgalway.finance.portfolio.domain.model.*;
import ie.universityofgalway.finance.portfolio.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ValuationController.class)
@Import({SecurityConfig.class, ValuationService.class})
@TestPropertySource(properties = "jwt.secret=test-only-secret-that-is-at-least-32-bytes-long")
class ValuationControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PortfolioService portfolios;
    @MockitoBean private PriceClient prices;

    private void holdings(String currency) {
        when(portfolios.getHoldings(5L, "alice")).thenReturn(List.of(
                new Holding(1L, 5L, "AAPL", new BigDecimal("2"), new BigDecimal("100"), currency)));
    }
    @Test void requiresAuthenticationBeforeReadingHoldings() throws Exception {
        mvc.perform(get("/api/portfolios/5/valuation")).andExpect(status().isUnauthorized());
        verifyNoInteractions(portfolios, prices);
    }
    @Test void responsePreservesQuoteAndSavedCostAndCalculatesTotals() throws Exception {
        holdings("USD");
        when(prices.getQuote("AAPL")).thenReturn(new MarketQuote("AAPL", new BigDecimal("125.5"),
                "USD", Instant.parse("2026-11-16T12:00:00Z"), true));
        mvc.perform(get("/api/portfolios/5/valuation").with(jwt().jwt(j -> j.subject("alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(true))
                .andExpect(jsonPath("$.stale").value(true))
                .andExpect(jsonPath("$.unavailableHoldings").value(0))
                .andExpect(jsonPath("$.currentValue").value(251))
                .andExpect(jsonPath("$.costBasis").value(200))
                .andExpect(jsonPath("$.unrealisedGainLoss").value(51))
                .andExpect(jsonPath("$.holdings[0].avgPurchasePrice").value(100))
                .andExpect(jsonPath("$.holdings[0].currency").value("USD"))
                .andExpect(jsonPath("$.holdings[0].valuationAvailable").value(true))
                .andExpect(jsonPath("$.holdings[0].quote.price").value(125.5))
                .andExpect(jsonPath("$.holdings[0].quote.currency").value("USD"))
                .andExpect(jsonPath("$.holdings[0].quote.asOf").value("2026-11-16T12:00:00Z"))
                .andExpect(jsonPath("$.holdings[0].quote.stale").value(true));
        verify(portfolios).getHoldings(5L, "alice");
    }
    @Test void pricingFailureReturnsSavedHoldingsWithNullTotals() throws Exception {
        holdings(null);
        when(prices.getQuote("AAPL")).thenThrow(new PriceUnavailableException("offline", null));
        mvc.perform(get("/api/portfolios/5/valuation").with(jwt().jwt(j -> j.subject("alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(false))
                .andExpect(jsonPath("$.currentValue").isEmpty())
                .andExpect(jsonPath("$.unavailableHoldings").value(1))
                .andExpect(jsonPath("$.holdings[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.holdings[0].quantity").value(2))
                .andExpect(jsonPath("$.holdings[0].valuationAvailable").value(false))
                .andExpect(jsonPath("$.holdings[0].currentValue").isEmpty())
                .andExpect(jsonPath("$.holdings[0].unavailableReason").value("QUOTE_UNAVAILABLE"));
    }
    @Test void otherOwnersPortfolioIsNotFoundBeforePricing() throws Exception {
        when(portfolios.getHoldings(5L, "mallory")).thenThrow(new PortfolioNotFoundException(5L));
        mvc.perform(get("/api/portfolios/5/valuation").with(jwt().jwt(j -> j.subject("mallory"))))
                .andExpect(status().isNotFound());
        verifyNoInteractions(prices);
    }
}
