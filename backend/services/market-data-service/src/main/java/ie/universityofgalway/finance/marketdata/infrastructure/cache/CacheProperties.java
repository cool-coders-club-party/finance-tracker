package ie.universityofgalway.finance.marketdata.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

//Reads the cache freshness period from application.yml.
@ConfigurationProperties(prefix = "market-data.cache")
public record CacheProperties(Duration freshness) {
}
