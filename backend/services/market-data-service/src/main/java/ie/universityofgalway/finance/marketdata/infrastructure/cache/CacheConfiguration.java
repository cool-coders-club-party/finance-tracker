package ie.universityofgalway.finance.marketdata.infrastructure.cache;

import ie.universityofgalway.finance.marketdata.domain.MarketDataProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

/**
 * Wires the live or stub provider behind the primary in-memory quote cache.
 */
@Configuration
public class CacheConfiguration {

    /**
     * Provides the UTC clock shared by quote caching and market-data services.
     *
     * @return the system UTC clock
     */
    @Bean
    Clock marketDataClock() {
        return Clock.systemUTC();
    }

    /**
     * Makes the caching decorator the default provider injected into quote lookup.
     *
     * @param liveProvider underlying provider selected by the active profile
     * @param properties configured cache freshness duration
     * @param marketDataClock clock used to measure time since a quote was cached
     * @return the primary caching market-data provider
     */
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
