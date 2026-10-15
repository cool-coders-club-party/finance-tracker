package ie.universityofgalway.finance.marketdata.infrastructure.cache;

import ie.universityofgalway.finance.marketdata.application.QuoteService;
import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import ie.universityofgalway.finance.marketdata.domain.Quote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
@ActiveProfiles("stub")
class CacheConfigurationTest {

    @Autowired
    private MarketDataProvider provider;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private CacheProperties properties;

    @Test
    void quoteServiceUsesConfiguredCache() {
        //Checks that Spring injects the cache and binds its freshness period.
        assertInstanceOf(CachingMarketDataProvider.class, provider);
        assertEquals(Duration.ofMinutes(15), properties.freshness());

        //Checks that repeated requests reuse the same cached quote.
        Quote first = quoteService.getQuote("AAPL").orElseThrow();
        Quote second = quoteService.getQuote("AAPL").orElseThrow();
        assertSame(first, second);
    }
}
