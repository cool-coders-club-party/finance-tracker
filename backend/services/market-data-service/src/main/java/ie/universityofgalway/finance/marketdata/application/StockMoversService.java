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

    public StockMoversService(QuoteService quoteService, Clock clock) {
        this.quoteService = quoteService;
        this.clock = clock;
    }

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

    public record Ranking(int trackedCount, int usableCount, List<MoverRow> rows) {}

    public record MoverRow(
            String ticker,
            String company,
            BigDecimal percentChange,
            @JsonFormat(shape = JsonFormat.Shape.STRING) Instant asOf,
            boolean stale
    ) {}

    private record TrackedStock(String ticker, String company) {}

    private record CachedRanking(Optional<Ranking> result, Instant expiresAt) {}
}
