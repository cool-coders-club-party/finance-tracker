package ie.universityofgalway.finance.marketdata.infrastructure.cache;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachingMarketDataProviderTest {

    @Test
    void returnsFreshCachedQuoteWithoutCallingProviderAgain() {
        //Creates a fixed time so the test result is predictable.
        Instant now = Instant.parse("2026-10-15T12:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        //Creates the quote that the external provider will return.
        Quote quote = new Quote(
                "AAPL",
                new BigDecimal("192.45"),
                now
        );

        //Creates a fake external provider that returns our test quote.
        MarketDataProvider liveProvider = mock(MarketDataProvider.class);
        when(liveProvider.findQuote("AAPL"))
                .thenReturn(Optional.of(quote));

        //Creates the cache with a fifteen-minute freshness period.
        CachingMarketDataProvider cachingProvider =
                new CachingMarketDataProvider(
                        liveProvider,
                        Duration.ofMinutes(15),
                        clock
                );

        //Requests the same quote twice.
        Optional<Quote> firstResult =
                cachingProvider.findQuote("AAPL");

        Optional<Quote> secondResult =
                cachingProvider.findQuote("AAPL");

        //Checks that both requests returned the expected quote.
        assertEquals(Optional.of(quote), firstResult);
        assertEquals(Optional.of(quote), secondResult);

        //Checks that the external provider was called only once.
        verify(liveProvider, times(1))
                .findQuote("AAPL");
    }

    @Test
    void refreshesQuoteWhenCachedEntryHasExpired() {
        //Defines when the quote is first cached.
        Instant initialTime =
                Instant.parse("2026-10-15T12:00:00Z");

        //Moves time beyond the fifteen-minute freshness period.
        Instant expiredTime =
                initialTime.plus(Duration.ofMinutes(16));

        //Creates a clock that returns controlled times during the test.
        Clock clock = mock(Clock.class);
        when(clock.instant())
                .thenReturn(
                        initialTime,
                        expiredTime,
                        expiredTime
                );

        //Creates the first quote returned by the provider.
        Quote initialQuote = new Quote(
                "AAPL",
                new BigDecimal("192.45"),
                initialTime
        );

        //Creates a newer quote for the refresh request.
        Quote refreshedQuote = new Quote(
                "AAPL",
                new BigDecimal("195.00"),
                expiredTime
        );

        //Makes the provider return a different quote on each call.
        MarketDataProvider liveProvider =
                mock(MarketDataProvider.class);

        when(liveProvider.findQuote("AAPL"))
                .thenReturn(
                        Optional.of(initialQuote),
                        Optional.of(refreshedQuote)
                );

        //Creates a cache whose entries remain fresh for fifteen minutes.
        CachingMarketDataProvider cachingProvider =
                new CachingMarketDataProvider(
                        liveProvider,
                        Duration.ofMinutes(15),
                        clock
                );

        //Stores the first provider result in the cache.
        Optional<Quote> firstResult =
                cachingProvider.findQuote("AAPL");

        //Requests the quote again after the cache has expired.
        Optional<Quote> secondResult =
                cachingProvider.findQuote("AAPL");

        //Checks that the first request returned the original quote.
        assertEquals(
                Optional.of(initialQuote),
                firstResult
        );

        //Checks that the expired quote was replaced by the newer quote.
        assertEquals(
                Optional.of(refreshedQuote),
                secondResult
        );

        //Checks that the live provider was called for both requests.
        verify(liveProvider, times(2))
                .findQuote("AAPL");
    }

    @Test
    void returnsStaleCachedQuoteWhenProviderFails() {
        //Defines when the quote first enters the cache.
        Instant initialTime =
                Instant.parse("2026-10-15T12:00:00Z");

        //Moves time beyond the cache freshness period.
        Instant expiredTime =
                initialTime.plus(Duration.ofMinutes(16));

        //Returns the initial time and then the expired time.
        Clock clock = mock(Clock.class);
        when(clock.instant())
                .thenReturn(
                        initialTime,
                        expiredTime
                );

        //Creates the quote that will become stale.
        Quote cachedQuote = new Quote(
                "AAPL",
                new BigDecimal("192.45"),
                initialTime
        );

        //Returns a quote first and simulates provider failure second.
        MarketDataProvider liveProvider =
                mock(MarketDataProvider.class);

        when(liveProvider.findQuote("AAPL"))
                .thenReturn(Optional.of(cachedQuote))
                .thenThrow(
                        new RestClientException(
                                "Provider unavailable"
                        )
                );

        //Creates a cache with a fifteen-minute freshness period.
        CachingMarketDataProvider cachingProvider =
                new CachingMarketDataProvider(
                        liveProvider,
                        Duration.ofMinutes(15),
                        clock
                );

        //Stores the successful provider result.
        Optional<Quote> firstResult =
                cachingProvider.findQuote("AAPL");

        //Requests the quote after expiry while the provider is unavailable.
        Optional<Quote> fallbackResult =
                cachingProvider.findQuote("AAPL");

        //Checks that the initial request returned the quote.
        assertEquals(
                Optional.of(cachedQuote),
                firstResult
        );

        //Checks that the stale quote was returned as a fallback.
        assertEquals(
                Optional.of(cachedQuote),
                fallbackResult
        );

        //Checks that the cache attempted to refresh the expired quote.
        verify(liveProvider, times(2))
                .findQuote("AAPL");
    }

}