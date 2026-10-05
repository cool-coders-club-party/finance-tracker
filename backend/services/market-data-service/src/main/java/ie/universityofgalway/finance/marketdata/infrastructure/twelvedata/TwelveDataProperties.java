package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Holds the Twelve Data settings from application-local.yml.
@ConfigurationProperties(prefix = "market-data.twelve-data")
public record TwelveDataProperties(
        String baseUrl,
        String apiKey
) {
}

