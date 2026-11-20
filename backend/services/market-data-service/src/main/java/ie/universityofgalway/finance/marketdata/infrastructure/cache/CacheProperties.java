package ie.universityofgalway.finance.marketdata.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds the {@code market-data.cache} configuration namespace.
 *
 * @param freshness duration for which a quote is fresh after entering the cache
 */
@ConfigurationProperties(prefix = "market-data.cache")
public record CacheProperties(Duration freshness) {
}
