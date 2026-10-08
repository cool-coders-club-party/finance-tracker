package ie.universityofgalway.finance.portfolio.config;

import ie.universityofgalway.finance.portfolio.model.Portfolio;
import ie.universityofgalway.finance.portfolio.repository.PortfolioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Seeds a test Portfolio on startup so there's something to call /buy and
 * /sell against immediately, without needing a POST /api/portfolios endpoint
 * yet. The userId here ("user-1") matches the hardcoded value currently in
 * PortfolioController - remove or replace this entirely once real portfolio
 * creation and authentication exist.
 */
@Configuration
@Profile("!prod") // never seed fake data into a real environment
public class DataSeeder {

    @Bean
    CommandLineRunner seedTestPortfolio(PortfolioRepository portfolioRepository) {
        return args -> {
            if (portfolioRepository.count() == 0) {
                Portfolio portfolio = portfolioRepository.save(new Portfolio("user-1", "Test Portfolio"));
                System.out.println("Seeded test portfolio with id=" + portfolio.getId());
            }
        };
    }
}
