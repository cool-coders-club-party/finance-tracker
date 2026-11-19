package ie.universityofgalway.finance.marketdata.application;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TopStocksServiceTest {
    private static final String SNAPSHOT = """
            {
              "capturedOn": "2026-11-10",
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
    void returnsFiveLargestCompaniesInDescendingOrder() {
        var result = service(SNAPSHOT).getTopStocks().orElseThrow();

        assertThat(result.rows())
                .extracting(TopStocksService.RankingRow::ticker)
                .containsExactly("AAA", "BBB", "CCC", "DDD", "EEE");
    }

    @Test
    void keepsHistoricalSnapshotAvailableAfterReload() {
        String historicalSnapshot = SNAPSHOT.replace("2026-11-10", "2000-01-01");
        var first = service(historicalSnapshot);
        var result = first.getTopStocks().orElseThrow();

        assertThat(result.capturedOn()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(first.getTopStocks()).contains(result);
        assertThat(service(historicalSnapshot).getTopStocks()).contains(result);
    }

    @Test
    void rejectsInvalidSnapshot() {
        String duplicateTicker = SNAPSHOT.replace("\"FFF\"", "\"AAA\"");
        assertThat(service(duplicateTicker).getTopStocks()).isEmpty();
    }

    @Test
    void loadsBundledSnapshotWithoutExpiry() throws Exception {
        var resource = new ClassPathResource("top-stocks-snapshot.json");
        var mapper = JsonMapper.builder().build();
        try (var input = resource.getInputStream()) {
            assertThat(mapper.readTree(input).has("expiresAt")).isFalse();
        }
        var service = new TopStocksService(resource, mapper);

        assertThat(service.getTopStocks().orElseThrow().rows()).hasSize(5);
    }

    private TopStocksService service(String json) {
        return new TopStocksService(
                new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)),
                JsonMapper.builder().build()
        );
    }
}
