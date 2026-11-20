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
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Loads and validates a manually maintained market-cap snapshot once at startup.
 *
 * <p>The largest five entries are served without live provider calls or automatic expiry.
 * Updating the bundled snapshot requires a deployment.</p>
 */
@Service
public class TopStocksService {
    private static final Logger log = LoggerFactory.getLogger(TopStocksService.class);
    private static final int RANKING_SIZE = 5;

    private final Optional<Snapshot> snapshot;

    /**
     * Loads the configured snapshot resource.
     *
     * @param resource bundled JSON snapshot
     * @param mapper JSON mapper used to read the snapshot
     */
    public TopStocksService(
            @Value("classpath:top-stocks-snapshot.json") Resource resource,
            JsonMapper mapper
    ) {
        this.snapshot = loadSnapshot(resource, mapper);
    }

    /**
     * Returns the snapshot loaded at startup.
     *
     * @return the five-stock snapshot, or an empty optional if loading or validation failed
     */
    public Optional<Snapshot> getTopStocks() {
        return snapshot;
    }

    /**
     * Reads, validates and ranks the snapshot, logging failures without preventing startup.
     *
     * @param resource JSON snapshot resource
     * @param mapper JSON mapper
     * @return the largest five entries, or an empty optional for an unreadable or invalid snapshot
     */
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
            return Optional.of(new Snapshot(data.capturedOn(), data.sourceUrl(), rows));
        } catch (IOException | JacksonException exception) {
            log.warn("Could not read top stocks snapshot", exception);
            return Optional.empty();
        }
    }

    /**
     * Checks capture date, HTTPS source, unique non-blank tickers and positive market caps.
     *
     * @param data snapshot to validate; may be null
     * @return true when all rows are valid and there are at least five entries
     */
    private boolean isValid(Snapshot data) {
        return data != null
                && data.capturedOn() != null
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

    /**
     * A dated market-cap snapshot with source attribution.
     *
     * @param capturedOn date the source was captured, rather than necessarily its trading date
     * @param sourceUrl HTTPS source of the captured ranking
     * @param rows snapshot entries; the service exposes the five largest by market cap
     */
    public record Snapshot(
            @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate capturedOn,
            String sourceUrl,
            List<RankingRow> rows
    ) {}

    /**
     * One company in the market-cap snapshot.
     *
     * @param ticker stock symbol
     * @param company company display name
     * @param marketCapUsd market capitalisation in whole US dollars
     */
    public record RankingRow(String ticker, String company, long marketCapUsd) {}
}
