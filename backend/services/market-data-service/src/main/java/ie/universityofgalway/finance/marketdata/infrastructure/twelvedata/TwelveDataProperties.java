package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code market-data.twelve-data} configuration namespace.
 *
 * @param baseUrl base URL of the Twelve Data API
 * @param apiKey API key sent in the Authorization header; must not be logged
 */
@ConfigurationProperties(prefix = "market-data.twelve-data")
public record TwelveDataProperties(
        String baseUrl,
        String apiKey
) {
}

