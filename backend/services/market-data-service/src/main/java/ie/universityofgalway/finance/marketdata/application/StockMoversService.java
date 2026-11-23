package ie.universityofgalway.finance.marketdata.application;

import com.fasterxml.jackson.annotation.JsonFormat;
import ie.universityofgalway.finance.marketdata.domain.MarketDataUnavailableException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Ranks a fixed eight-stock watchlist by the absolute daily percentage change.
 *
 * <p>Successful non-empty rankings are cached for 15 minutes. Empty or failed results
 * are retained for one minute to limit retries and external API usage.</p>
 */
@Service
public class StockMoversService {
    private static final Duration FRESHNESS = Duration.ofMinutes(15);
    private static final Duration RETRY_COOLDOWN = Duration.ofMinutes(1);

    // Keep the free-tier watchlist fixed so a refresh has a bounded quote cost.
    private static final List<TrackedStock> WATCHLIST = List.of(
            new TrackedStock("AAPL", "Apple"),
            new TrackedStock("MSFT", "Microsoft"),
            new TrackedStock("NVDA", "NVIDIA"),
            new TrackedStock("AMZN", "Amazon"),
            new TrackedStock("GOOGL", "Alphabet"),
            new TrackedStock("META", "Meta Platforms"),
            new TrackedStock("TSLA", "Tesla"),
            new TrackedStock("JPM", "JPMorgan Chase")
    );

    private final QuoteService quoteService;
    private final Clock clock;
    private CachedRanking cachedRanking;

    /**
     * Creates the ranking service with a controllable cache clock.
     *
     * @param quoteService quote lookup service, including quote caching
     * @param clock clock used to expire ranking results
     */
    public StockMoversService(QuoteService quoteService, Clock clock) {
        this.quoteService = quoteService;
        this.clock = clock;
    }

    /**
     * Returns a cached ranking or refreshes it once for concurrent callers.
     *
     * <p>Each usable quote must include a daily percentage change and timestamp.
     * Rows are ordered by absolute change descending, then ticker ascending.</p>
     *
     * @return up to five movers with coverage counts; an empty optional if provider failures leave no usable
     *     candidates
     */
    public synchronized Optional<Ranking> getMovers() {
        // Share one refresh across concurrent visitors, including during provider outages.
        if (cachedRanking != null && clock.instant().isBefore(cachedRanking.expiresAt())) {
            return cachedRanking.result();
        }

        Optional<Ranking> result = buildRanking();
        Duration lifetime = result.filter(ranking -> !ranking.rows().isEmpty()).isPresent()
                ? FRESHNESS
                : RETRY_COOLDOWN;
        cachedRanking = new CachedRanking(result, clock.instant().plus(lifetime));
        return result;
    }

    /**
     * Collects usable watchlist quotes, tolerating individual provider failures.
     *
     * @return the ranked candidates, possibly with no rows, or an empty optional when all usable data is lost
     *     to provider failures
     */
    private Optional<Ranking> buildRanking() {
        List<MoverRow> candidates = new ArrayList<>();
        boolean providerUnavailable = false;

        for (TrackedStock stock : WATCHLIST) {
            try {
                quoteService.getQuote(stock.ticker()).ifPresent(quote -> {
                    var details = quote.details();
                    if (details != null && details.percentChange() != null && quote.asOf() != null) {
                        String name = details.name();
                        candidates.add(new MoverRow(
                                stock.ticker(), name == null || name.isBlank() ? stock.company() : name,
                                details.percentChange(), quote.asOf(), quote.stale()
                        ));
                    }
                });
            } catch (MarketDataUnavailableException exception) {
                // One failed lookup should not discard the quotes we already received.
                providerUnavailable = true;
            }
        }

        if (candidates.isEmpty() && providerUnavailable) {
            return Optional.empty();
        }

        // Compare the size of each move and preserve its sign for display.
        List<MoverRow> rows = candidates.stream()
                .sorted(Comparator.comparing((MoverRow row) -> row.percentChange().abs())
                        .reversed()
                        .thenComparing(MoverRow::ticker))
                .limit(5)
                .toList();
        return Optional.of(new Ranking(WATCHLIST.size(), candidates.size(), rows));
    }

    /**
     * A ranking with watchlist coverage counts.
     *
     * @param trackedCount number of stocks in the fixed watchlist
     * @param usableCount number of usable quotes before limiting the displayed rows
     * @param rows up to five movers ordered by absolute daily percentage change
     */
    public record Ranking(int trackedCount, int usableCount, List<MoverRow> rows) {}

    /**
     * One tracked stock's daily movement.
     *
     * @param ticker stock symbol
     * @param company provider name, or the configured watchlist name if absent
     * @param percentChange signed daily percentage change, where 8 means 8 percent
     * @param asOf timestamp of the underlying quote
     * @param stale whether the underlying quote is a cached fallback
     */
    public record MoverRow(
            String ticker,
            String company,
            BigDecimal percentChange,
            @JsonFormat(shape = JsonFormat.Shape.STRING) Instant asOf,
            boolean stale
    ) {}

    /**
     * A watchlist entry and its fallback display name.
     *
     * @param ticker symbol to query
     * @param company display name used when the quote omits one
     */
    private record TrackedStock(String ticker, String company) {}

    /**
     * A ranking result, including unavailability, retained until the next refresh.
     *
     * @param result cached ranking or absence caused by provider failures
     * @param expiresAt instant at which the result must be rebuilt
     */
    private record CachedRanking(Optional<Ranking> result, Instant expiresAt) {}
}
