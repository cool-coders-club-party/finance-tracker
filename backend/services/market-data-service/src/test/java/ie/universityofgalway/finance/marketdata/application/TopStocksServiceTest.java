package ie.universityofgalway.finance.marketdata.application;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TopStocksServiceTest {
    private static final Instant EXPIRY = Instant.parse("2026-11-11T21:00:00Z");
    private static final String SNAPSHOT = """
            {
              "capturedOn": "2026-11-10",
              "expiresAt": "2026-11-11T21:00:00Z",
              "sourceUrl": "https://example.com/stocks",
              "rows": [
                {"ticker": "DDD", "company": "Company D", "marketCapUsd": 300},
                {"ticker": "FFF", "company": "Company F", "marketCapUsd": 100},
                {"ticker": "AAA", "company": "Company A", "marketCapUsd": 600},
                {"ticker": "EEE", "company": "Company E", "marketCapUsd": 200},
                {"ticker": "CCC", "company": "Company C", "marketCapUsd": 400},
                {"ticker": "BBB", "company": "Company B", "marketCapUsd": 500}
              ]
            }
            """;

    @Test
    void returnsFiveLargestCompaniesInDescendingOrderBeforeExpiry() {
        var result = service(SNAPSHOT, EXPIRY.minusNanos(1)).getTopStocks().orElseThrow();

        assertThat(result.rows())
                .extracting(TopStocksService.RankingRow::ticker)
                .containsExactly("AAA", "BBB", "CCC", "DDD", "EEE");
    }

    @Test
    void stopsServingSnapshotAtExpiry() {
        assertThat(service(SNAPSHOT, EXPIRY).getTopStocks()).isEmpty();
    }

    @Test
    void rejectsInvalidSnapshot() {
        String duplicateTicker = SNAPSHOT.replace("\"FFF\"", "\"AAA\"");
        assertThat(service(duplicateTicker, EXPIRY.minusSeconds(1)).getTopStocks()).isEmpty();
    }

    @Test
    void loadsBundledSnapshotOnItsCaptureDate() throws Exception {
        var resource = new ClassPathResource("top-stocks-snapshot.json");
        var mapper = JsonMapper.builder().build();
        LocalDate capturedOn;
        try (var input = resource.getInputStream()) {
            capturedOn = LocalDate.parse(mapper.readTree(input).path("capturedOn").asText());
        }
        var service = new TopStocksService(
                resource,
                mapper,
                Clock.fixed(capturedOn.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
        );

        assertThat(service.getTopStocks().orElseThrow().rows()).hasSize(5);
    }

    private TopStocksService service(String json, Instant now) {
        return new TopStocksService(
                new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)),
                JsonMapper.builder().build(),
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }
}
