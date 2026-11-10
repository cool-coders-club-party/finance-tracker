package ie.universityofgalway.finance.marketdata.application;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class TopStocksService {
    private static final Logger log = LoggerFactory.getLogger(TopStocksService.class);
    private static final int RANKING_SIZE = 5;

    private final Clock clock;
    private final Optional<Snapshot> snapshot;

    public TopStocksService(
            @Value("classpath:top-stocks-snapshot.json") Resource resource,
            JsonMapper mapper,
            Clock clock
    ) {
        this.clock = clock;
        this.snapshot = loadSnapshot(resource, mapper);
    }

    public Optional<Snapshot> getTopStocks() {
        // Check on every request because the bundled snapshot can expire while the service runs.
        return snapshot.filter(ranking -> clock.instant().isBefore(ranking.expiresAt()));
    }

    private Optional<Snapshot> loadSnapshot(Resource resource, JsonMapper mapper) {
        // The file changes with a deployment, so read and rank it once at startup.
        try (var input = resource.getInputStream()) {
            Snapshot data = mapper.readValue(input, Snapshot.class);
            if (!isValid(data)) {
                log.warn("Top stocks snapshot is invalid");
                return Optional.empty();
            }

            List<RankingRow> rows = data.rows().stream()
                    .sorted(Comparator.comparingLong(RankingRow::marketCapUsd).reversed())
                    .limit(RANKING_SIZE)
                    .toList();
            return Optional.of(new Snapshot(data.capturedOn(), data.expiresAt(), data.sourceUrl(), rows));
        } catch (IOException | JacksonException exception) {
            log.warn("Could not read top stocks snapshot", exception);
            return Optional.empty();
        }
    }

    private boolean isValid(Snapshot data) {
        return data != null
                && data.capturedOn() != null
                && data.expiresAt() != null
                && data.expiresAt().isAfter(data.capturedOn().atStartOfDay(ZoneOffset.UTC).toInstant())
                && data.sourceUrl() != null
                && data.sourceUrl().startsWith("https://")
                && data.rows() != null
                && data.rows().size() >= RANKING_SIZE
                && data.rows().stream().allMatch(row -> row != null
                        && row.ticker() != null && !row.ticker().isBlank()
                        && row.company() != null && !row.company().isBlank()
                        && row.marketCapUsd() > 0)
                && data.rows().stream().map(RankingRow::ticker).distinct().count() == data.rows().size();
    }

    public record Snapshot(
            @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate capturedOn,
            @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresAt,
            String sourceUrl,
            List<RankingRow> rows
    ) {}

    public record RankingRow(String ticker, String company, long marketCapUsd) {}
}
