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
}