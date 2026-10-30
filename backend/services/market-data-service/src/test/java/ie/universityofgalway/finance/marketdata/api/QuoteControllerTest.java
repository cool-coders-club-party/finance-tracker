package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.application.QuoteService;
import ie.universityofgalway.finance.marketdata.domain.MarketDataUnavailableException;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import ie.universityofgalway.finance.marketdata.domain.QuoteDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.mock;

public class QuoteControllerTest {

    private QuoteService quoteService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        //replace real service with a controllable mock
        quoteService = mock(QuoteService.class);

        //create the controller and a simulated web environment
        QuoteController quoteController = new QuoteController(quoteService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(quoteController)
                .build();
    }

    @Test
    void getQuoteReturnsOkAndQuoteWhenSymbolExists() throws Exception {
        //arrange
        Instant timestamp = Instant.parse("2026-09-30T10:00:00Z");

        QuoteDetails details = new QuoteDetails(
                "Apple Inc.",
                "USD",
                new BigDecimal("190.10"),
                null,
                null,
                183055376L,
                new BigDecimal("190.72"),
                new BigDecimal("1.73"),
                new BigDecimal("0.91"),
                null,
                null
        );

        Quote quote = new Quote(
                "AAPL",
                new BigDecimal("192.45"),
                timestamp,
                false,
                details
        );

        when(quoteService.getQuote("AAPL"))
                .thenReturn(Optional.of(quote));

        //act and assert: simulate GET /api/quotes/AAPL
        mockMvc.perform(get("/api/quotes/{symbol}", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.price").value(192.45))
                .andExpect(jsonPath("$.asOf").value(timestamp.toString()))
                .andExpect(jsonPath("$.stale").value(false))
                .andExpect(jsonPath("$.details.name").value("Apple Inc."))
                .andExpect(jsonPath("$.details.currency").value("USD"))
                .andExpect(jsonPath("$.details.open").value(190.10))
                .andExpect(jsonPath("$.details.volume").value(183055376))
                .andExpect(jsonPath("$.details.previousClose").value(190.72))
                .andExpect(jsonPath("$.details.change").value(1.73))
                .andExpect(jsonPath("$.details.percentChange").value(0.91));

        //confirm that the controller passed the path variable to the service
        verify(quoteService).getQuote("AAPL");

    }

    @Test
    void getQuoteMarksFallbackPriceAsStale() throws Exception {
        //Creates a cached fallback with its original price and timestamp.
        Instant timestamp = Instant.parse("2026-09-30T10:00:00Z");
        Quote staleQuote = new Quote(
                "AAPL", new BigDecimal("192.45"), timestamp, true
        );
        when(quoteService.getQuote("AAPL"))
                .thenReturn(Optional.of(staleQuote));

        //Checks that clients can recognise an older fallback price.
        mockMvc.perform(get("/api/quotes/{symbol}", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stale").value(true))
                .andExpect(jsonPath("$.asOf").value(timestamp.toString()));
    }

    @Test
    void getQuoteReturnsNotFoundWhenSymbolDoesNotExist() throws Exception {
        //arrange: the mocked service cannot find the requested symbol
        when(quoteService.getQuote("NOPE"))
                .thenReturn(Optional.empty());

        //act and assert: simulate GET /api/quotes/NOPE
        mockMvc.perform(get("/api/quotes/{symbol}", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("No quote found for symbol: NOPE")
                );

        //confirm that the controller passed the symbol to the service
        verify(quoteService).getQuote("NOPE");
    }

    @Test
    void getQuoteReturnsServiceUnavailableWhenProviderFails() throws Exception {
        //Arrange: simulate a provider failure when no cached quote is available.
        MarketDataUnavailableException exception =
                new MarketDataUnavailableException(
                        new RuntimeException("Provider failed")
                );

        when(quoteService.getQuote("AAPL"))
                .thenThrow(exception);

        //Act and assert: the API should return a clear temporary failure response.
        mockMvc.perform(get("/api/quotes/{symbol}", "AAPL"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title")
                        .value("Service Unavailable"))
                .andExpect(jsonPath("$.detail")
                        .value("Market data is temporarily unavailable"));

        //Confirm that the requested symbol reached the service.
        verify(quoteService).getQuote("AAPL");
    }

}


