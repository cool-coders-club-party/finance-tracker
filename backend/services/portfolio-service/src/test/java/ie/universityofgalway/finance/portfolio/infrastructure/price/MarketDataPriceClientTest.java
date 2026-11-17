package ie.universityofgalway.finance.portfolio.infrastructure.price;

import ie.universityofgalway.finance.portfolio.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MarketDataPriceClientTest {
    private MockRestServiceServer server;
    private MarketDataPriceClient client;
    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("http://market-data");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MarketDataPriceClient(builder.build());
    }
    @Test void readsNestedCurrencyAndPreservesPriceTimestampAndStaleness() {
        server.expect(requestTo("http://market-data/api/quotes/AAPL")).andRespond(withSuccess(
                "{\"symbol\":\"AAPL\",\"price\":120.25,\"asOf\":\"2026-11-16T12:00:00Z\",\"stale\":true,\"details\":{\"currency\":\"USD\"}}", MediaType.APPLICATION_JSON));
        var quote = client.getQuote("AAPL");
        assertThat(quote.price()).isEqualByComparingTo("120.25");
        assertThat(quote.currency()).isEqualTo("USD");
        assertThat(quote.asOf()).isEqualTo(Instant.parse("2026-11-16T12:00:00Z"));
        assertThat(quote.stale()).isTrue();
        server.verify();
    }
    @Test void missingDetailsDoesNotInventCurrency() {
        server.expect(anything()).andRespond(withSuccess(
                "{\"symbol\":\"AAPL\",\"price\":120,\"asOf\":\"2026-11-16T12:00:00Z\",\"stale\":false}", MediaType.APPLICATION_JSON));
        assertThat(client.getQuote("AAPL").currency()).isNull();
    }
    @Test void maps404ToUnknownTicker() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> client.getQuote("AAPL")).isInstanceOf(InvalidSymbolException.class);
    }
    @Test void maps503ToUnavailable() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.getQuote("AAPL")).isInstanceOf(PriceUnavailableException.class);
    }
    @Test void mapsTransportFailureToUnavailable() {
        server.expect(anything()).andRespond(withException(new IOException("timeout")));
        assertThatThrownBy(() -> client.getQuote("AAPL")).isInstanceOf(PriceUnavailableException.class);
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "null", "not json", "{}",
            "{\"symbol\":\"AAPL\",\"price\":-1,\"asOf\":\"2026-11-16T12:00:00Z\",\"stale\":false}",
            "{\"symbol\":\"MSFT\",\"price\":120,\"asOf\":\"2026-11-16T12:00:00Z\",\"stale\":false}",
            "{\"symbol\":\"AAPL\",\"price\":120,\"stale\":false}",
            "{\"symbol\":\"AAPL\",\"price\":120,\"asOf\":\"2026-11-16T12:00:00Z\"}"})
    void rejectsIncompleteOrMalformedQuote(String json) {
        server.expect(anything()).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.getQuote("AAPL")).isInstanceOf(PriceUnavailableException.class);
    }
}
