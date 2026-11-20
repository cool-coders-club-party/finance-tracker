package ie.universityofgalway.finance.marketdata.infrastructure.cache;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import ie.universityofgalway.finance.marketdata.domain.MarketDataUnavailableException;

import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches quotes by ticker and retains expired entries as fallbacks for HTTP provider failures.
 *
 * <p>Freshness is measured from cache insertion, not from the provider timestamp.
 * A missing live quote is returned as absence rather than replaced by an old quote.</p>
 */
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

    /**
     * Creates a cache around the supplied provider.
     *
     * @param liveProvider provider called when no fresh entry exists
     * @param freshnessPeriod freshness duration measured from insertion
     * @param clock clock used for insertion times and expiry checks
     */
    public CachingMarketDataProvider(
            MarketDataProvider liveProvider,
            Duration freshnessPeriod,
            Clock clock
    ) {
        this.liveProvider = liveProvider;
        this.freshnessPeriod = freshnessPeriod;
        this.clock = clock;
    }

    /**
     * Returns a fresh cached quote or requests a replacement.
     *
     * <p>If the provider throws {@link RestClientException}, an existing entry is returned
     * as stale regardless of age. With no cached entry, the failure becomes a domain exception.
     * Concurrent requests may independently refresh the same expired symbol.</p>
     *
     * @param symbol non-null normalised ticker used as the cache key
     * @return a live or cached quote, or an empty optional when the live provider has no quote
     * @throws MarketDataUnavailableException if the HTTP provider fails and no cached quote exists
     */
    @Override
    public Optional<Quote> findQuote(String symbol) {
        //Looks for an existing cached quote.
        CacheEntry cachedEntry = cache.get(symbol);

        //Returns the cached quote when it is still fresh.
        if (cachedEntry != null && isFresh(cachedEntry)) {
            return Optional.of(cachedEntry.quote());
        }

        try {
            //Requests a new quote from the external provider.
            Optional<Quote> liveResult =
                    liveProvider.findQuote(symbol);

            //Stores a successful result with the current cache time.
            liveResult.ifPresent(quote ->
                    cache.put(
                            symbol,
                            new CacheEntry(
                                    quote,
                                    clock.instant()
                            )
                    )
            );

            return liveResult;
        } catch (RestClientException exception) {
            //Returns the older quote when the provider is unavailable.
            if (cachedEntry != null) {
                //Marks the cached quote as stale before returning the fallback.
                return Optional.of(cachedEntry.quote().asStale());
            }

            //Convert the provider error into a meaningful domain-level failure.
            throw new MarketDataUnavailableException(exception);
        }
    }

    /**
     * Checks expiry against the injected clock; an entry is expired at the exact expiry instant.
     *
     * @param entry cached quote and its insertion time
     * @return true while the insertion time plus freshness duration is strictly in the future
     */
    private boolean isFresh(CacheEntry entry) {
        //Calculates the exact time when this cache entry expires.
        Instant expiresAt =
                entry.cachedAt().plus(freshnessPeriod);

        return expiresAt.isAfter(clock.instant());
    }

    /**
     * A quote and the time it entered this cache.
     *
     * @param quote original provider quote
     * @param cachedAt insertion time used for cache expiry
     */
    private record CacheEntry(
            Quote quote,
            Instant cachedAt
    ) {
    }
}
