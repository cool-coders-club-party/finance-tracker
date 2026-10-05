package ie.universityofgalway.finance.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class MarketDataServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketDataServiceApplication.class, args);
    }
}
