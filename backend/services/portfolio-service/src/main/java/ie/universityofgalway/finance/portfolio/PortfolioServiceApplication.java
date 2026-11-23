package ie.universityofgalway.finance.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the portfolio service.
 *
 * <p>Records each user's portfolios, holdings and trades, values holdings using quotes from
 * market-data-service and calculates profit and loss from trade history. Serves its API on
 * port 8083 under {@code /api/portfolios}.</p>
 */
@SpringBootApplication
public class PortfolioServiceApplication {
    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments, passed on to Spring (e.g. {@code --server.port=9000})
     */
    public static void main(String[] args) {
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}
