package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import ie.universityofgalway.finance.marketdata.domain.PricePoint;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TwelveDataPriceHistoryProviderTest {

    @Test
    void returnsDailyClosesOldestFirst() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // Fix today so the one-month request is predictable.
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-30T12:00:00Z"),
                ZoneOffset.UTC
        );

        TwelveDataPriceHistoryProvider provider = new TwelveDataPriceHistoryProvider(
                builder,
                new TwelveDataProperties("https://api.twelvedata.com", "test-key"),
                clock
        );

        // Twelve Data sends the newest day first.
        String responseBody = """
                {
                  "status": "ok",
                  "values": [
                    { "datetime": "2026-10-29", "close": "348.91" },
                    { "datetime": "2026-10-28", "close": "343.83" }
                  ]
                }
                """;

        server.expect(requestTo(
                        "https://api.twelvedata.com/time_series"
                        + "?symbol=AAPL&interval=1day"
                        + "&start_date=2026-09-30&end_date=2026-10-30"
                ))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<PricePoint> points = provider.findDailyHistory("AAPL").orElseThrow();

        assertEquals(List.of(
                new PricePoint(LocalDate.parse("2026-10-28"), new BigDecimal("343.83")),
                new PricePoint(LocalDate.parse("2026-10-29"), new BigDecimal("348.91"))
        ), points);

        server.verify();
    }
}