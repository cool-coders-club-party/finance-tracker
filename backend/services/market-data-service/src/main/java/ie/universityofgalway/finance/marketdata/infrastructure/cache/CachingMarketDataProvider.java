package ie.universityofgalway.finance.marketdata.infrastructure.cache;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CachingMarketDataProvider implements MarketDataProvider {

    //Provides fresh quotes when the cache cannot be used.
    private final MarketDataProvider liveProvider;

    //Defines how long a cached quote remains fresh.
    private final Duration freshnessPeriod;

    //Provides the current time and can be controlled during tests.
    private final Clock clock;

    //Stores one cached quote for each ticker symbol.
    //A ConcurrentHashMap is a thread-safe version of a normal HashMap - safe if two users request the same quote at the same time
    private final Map<String, CacheEntry> cache =
            new ConcurrentHashMap<>();

    public CachingMarketDataProvider(
            MarketDataProvider liveProvider,
            Duration freshnessPeriod,
            Clock clock
    ) {
        this.liveProvider = liveProvider;
        this.freshnessPeriod = freshnessPeriod;
        this.clock = clock;
    }

    @Override
    public Optional<Quote> findQuote(String symbol) {
        //Looks for an existing cached quote.
        CacheEntry cachedEntry = cache.get(symbol);

        //Returns the cached quote when it is still fresh.
        if (cachedEntry != null && isFresh(cachedEntry)) {
            return Optional.of(cachedEntry.quote());
        }

        //Requests a new quote when the cache is missing or expired.
        Optional<Quote> liveResult =
                liveProvider.findQuote(symbol);

        //Stores a successful result with the current cache time.
        liveResult.ifPresent(quote ->
                cache.put(
                        symbol,
                        new CacheEntry(quote, clock.instant())
                )
        );

        return liveResult;
    }

    private boolean isFresh(CacheEntry entry) {
        //Calculates the exact time when this cache entry expires.
        Instant expiresAt =
                entry.cachedAt().plus(freshnessPeriod);

        return expiresAt.isAfter(clock.instant());
    }

    //Keeps the quote together with the time it entered the cache.
    private record CacheEntry(
            Quote quote,
            Instant cachedAt
    ) {
    }
}
