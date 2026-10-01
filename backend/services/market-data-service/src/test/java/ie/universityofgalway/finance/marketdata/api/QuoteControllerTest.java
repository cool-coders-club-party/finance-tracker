package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.application.QuoteService;
import ie.universityofgalway.finance.marketdata.domain.Quote;
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

        Quote quote = new Quote(
                "AAPL",
                new BigDecimal("192.45"),
                timestamp
        );

        when(quoteService.getQuote("AAPL"))
                .thenReturn(Optional.of(quote));

        //act and assert: simulate GET /api/quotes/AAPL
        mockMvc.perform(get("/api/quotes/{symbol}", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.price").value(192.45))
                .andExpect(jsonPath("$.asOf").value(timestamp.toString()));

        //confirm that the controller passed the path variable to the service
        verify(quoteService).getQuote("AAPL");

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

}


