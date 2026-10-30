package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TwelveDataMarketDataProviderTest {

    private MockRestServiceServer server;
    private TwelveDataMarketDataProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();

        // Intercept requests instead of contacting the real API.
        server = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        TwelveDataProperties properties = new TwelveDataProperties(
                "https://api.twelvedata.com",
                "test-api-key"
        );

        provider = new TwelveDataMarketDataProvider(
                restClientBuilder,
                properties
        );
    }

    @Test
    void findQuoteMapsSuccessfulResponseToDomainQuote() {
        // Arrange: prepare a realistic Twelve Data response.
        String responseBody = """
                {
                  "symbol": "AAPL",
                  "name": "Apple Inc.",
                  "currency": "USD",
                  "close": "192.45",
                  "timestamp": 1760000000,
                  "open": "190.10",
                  "high": "193.00",
                  "low": "189.80",
                  "volume": "183055376",
                  "previous_close": "190.72",
                  "change": "1.73",
                  "percent_change": "0.91",
                  "fifty_two_week": {
                    "low": "150.00",
                    "high": "205.00"
                  }
                }
                """;

        server.expect(requestTo(
                        "https://api.twelvedata.com/quote?symbol=AAPL"
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "apikey test-api-key"
                ))
                .andRespond(withSuccess(
                        responseBody,
                        MediaType.APPLICATION_JSON
                ));

        // Act: perform the quote lookup.
        Optional<Quote> result = provider.findQuote("AAPL");

        // Assert: verify the provider-specific response was converted correctly.
        assertTrue(result.isPresent());

        Quote quote = result.get();
        assertEquals("AAPL", quote.symbol());
        assertEquals(new BigDecimal("192.45"), quote.price());
        assertEquals(
                Instant.ofEpochSecond(1760000000L),
                quote.asOf()
        );
        assertEquals("Apple Inc.", quote.details().name());
        assertEquals("USD", quote.details().currency());
        assertEquals(new BigDecimal("190.10"), quote.details().open());
        assertEquals(new BigDecimal("190.72"), quote.details().previousClose());
        assertEquals(new BigDecimal("1.73"), quote.details().change());
        assertEquals(new BigDecimal("0.91"), quote.details().percentChange());
        assertEquals(183055376L, quote.details().volume());
        assertEquals(new BigDecimal("150.00"), quote.details().fiftyTwoWeekLow());
        assertEquals(new BigDecimal("205.00"), quote.details().fiftyTwoWeekHigh());

        // Confirm that the expected HTTP request occurred.
        server.verify();
    }

    @Test
    void findQuoteReturnsEmptyWhenSymbolIsNotFound() {
        // Arrange: Twelve Data reports that the ticker does not exist.
        String responseBody = """
                {
                  "code": 404,
                  "message": "Symbol not found",
                  "status": "error"
                }
                """;

        server.expect(requestTo(
                        "https://api.twelvedata.com/quote?symbol=NOPE"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody));

        // Act: request an unknown ticker.
        Optional<Quote> result = provider.findQuote("NOPE");

        // Assert: an unknown ticker is a normal empty result.
        assertTrue(result.isEmpty());
        server.verify();
    }

    @Test
    void findQuoteRethrowsRateLimitError() {
        // Arrange: Twelve Data reports that the request limit was reached.
        String responseBody = """
                {
                  "code": 429,
                  "message": "API rate limit reached",
                  "status": "error"
                }
                """;

        server.expect(requestTo(
                        "https://api.twelvedata.com/quote?symbol=AAPL"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody));

        // Act and assert: provider failures must remain visible to the cache layer.
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> provider.findQuote("AAPL")
        );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getStatusCode()
        );

        server.verify();
    }

    @Test
    void findQuoteWorksWhenOptionalDetailsAreMissing() {
        String responseBody = """
                {
                  "symbol": "AAPL",
                  "close": "192.45",
                  "timestamp": 1760000000
                }
                """;

        server.expect(requestTo("https://api.twelvedata.com/quote?symbol=AAPL"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        Quote quote = provider.findQuote("AAPL").orElseThrow();

        assertEquals(new BigDecimal("192.45"), quote.price());
        assertEquals(null, quote.details().name());
        assertEquals(null, quote.details().fiftyTwoWeekLow());
        server.verify();
    }
}