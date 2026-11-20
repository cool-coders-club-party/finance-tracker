package ie.universityofgalway.finance.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Starts the Market Data service with configuration-property binding and Spring caching enabled.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class MarketDataServiceApplication {
    /**
     * Bootstraps the Spring application context and embedded web server.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(MarketDataServiceApplication.class, args);
    }
}
