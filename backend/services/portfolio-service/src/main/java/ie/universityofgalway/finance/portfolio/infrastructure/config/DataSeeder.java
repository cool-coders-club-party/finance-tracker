package ie.universityofgalway.finance.portfolio.infrastructure.config;

import ie.universityofgalway.finance.portfolio.application.port.PortfolioRepository;
import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
public class DataSeeder {

    @Bean
    CommandLineRunner seedTestPortfolio(PortfolioRepository portfolioRepository) {
        return args -> {
            if (portfolioRepository.findByUserId("user-1").isEmpty()) {
                Portfolio portfolio = portfolioRepository.save(Portfolio.create("user-1", "Test Portfolio"));
                System.out.println("Seeded test portfolio with id=" + portfolio.getId());
            }
        };
    }
}