package ie.universityofgalway.finance.marketdata.infrastructure.cache;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

@Configuration
public class CacheConfiguration {

    //Provides a clock that can also be replaced in integration tests.
    @Bean
    Clock marketDataClock() {
        return Clock.systemUTC();
    }

    //Makes the cache the provider used by QuoteService.
    @Bean
    @Primary
    MarketDataProvider cachingMarketDataProvider(
            @Qualifier("liveMarketDataProvider") MarketDataProvider liveProvider,
            CacheProperties properties,
            Clock marketDataClock
    ) {
        return new CachingMarketDataProvider(
                liveProvider,
                properties.freshness(),
                marketDataClock
        );
    }
}
