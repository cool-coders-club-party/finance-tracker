package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
                  "currency": "USD",
                  "close": "192.45",
                  "timestamp": 1760000000
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

        // Confirm that the expected HTTP request occurred.
        server.verify();
    }
}