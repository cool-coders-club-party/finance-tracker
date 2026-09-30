package ie.universityofgalway.finance.marketdata.application;

import ie.universityofgalway.finance.marketdata.MarketDataServiceApplication;
import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

public class QuoteServiceTest {

    private MarketDataProvider marketDataProvider;
    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        //create a controllable fake provider for each test with mockito
        marketDataProvider = mock(MarketDataProvider.class);

        //test QuoteService by passing fake mockito provider into it
        quoteService = new QuoteService(marketDataProvider);

    }

    @Test
    void getQuoteCleansSymbolAndReturnsProviderQuote() {
        //arrange: decide what the fake provider returns for AAPL.
        Instant timestamp = Instant.parse("2026-09-30T10:00:00Z");

        Quote expectedQuote = new Quote(
                "AAPL",
                new BigDecimal("192.45"),
                timestamp
        );

        when(marketDataProvider.findQuote("AAPL"))
                .thenReturn(Optional.of(expectedQuote));

        //act: call the service with untidy user input.
        Optional<Quote> result = quoteService.getQuote("  aapl  ");

        //assert: check the returned quote.
        assertTrue(result.isPresent());
        assertEquals(expectedQuote, result.get());

        //confirm that the provider received the cleaned ticker.
        verify(marketDataProvider).findQuote("AAPL");

    }

    @Test
    void getQuoteReturnsEmptyForNullSymbol() {
        //act: request a quote without providing a symbol.
        Optional<Quote> result = quoteService.getQuote(null);

        //assert: null input cannot produce a quote.
        assertTrue(result.isEmpty());

        //invalid input should never consume a provider request.
        verifyNoInteractions(marketDataProvider);
    }

    @Test
    void getQuoteReturnsEmptyForBlankSymbol() {
        //act: request a quote using only whitespace.
        Optional<Quote> result = quoteService.getQuote("   ");

        //assert: whitespace does not represent a valid ticker.
        assertTrue(result.isEmpty());

        //reject blank input before contacting the provider.
        verifyNoInteractions(marketDataProvider);
    }

    @Test
    void getQuoteReturnsEmptyWhenProviderCannotFindSymbol(){
        //arrange: the provider has no quote for this symbol.
        when(marketDataProvider.findQuote("NOPE"))
                .thenReturn(Optional.empty());

        //act: the service cleans the lowercase input before lookup.
        Optional<Quote> result = quoteService.getQuote("nope");

        //assert: the provider's empty result is returned by the service.
        assertTrue(result.isEmpty());

        //a valid-looking symbol should be passed to the provider.
        verify(marketDataProvider).findQuote("NOPE");
    }

}
