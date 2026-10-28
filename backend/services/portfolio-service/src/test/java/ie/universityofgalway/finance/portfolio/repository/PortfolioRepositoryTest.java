package ie.universityofgalway.finance.portfolio.repository;

import ie.universityofgalway.finance.portfolio.application.port.PortfolioRepository;
import ie.universityofgalway.finance.portfolio.domain.model.Portfolio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PortfolioRepositoryTest {

    @Autowired private PortfolioRepository portfolioRepository;

    @Test
    void findByUserIdReturnsOnlyThatUsersPortfolios() {
        portfolioRepository.save(new Portfolio("user-1", "Main"));
        portfolioRepository.save(new Portfolio("user-1", "Retirement"));
        portfolioRepository.save(new Portfolio("user-2", "Someone Else's"));

        assertThat(portfolioRepository.findByUserId("user-1"))
                .extracting(Portfolio::getName)
                .containsExactlyInAnyOrder("Main", "Retirement");
        assertThat(portfolioRepository.findByUserId("user-2"))
                .extracting(Portfolio::getName)
                .containsExactly("Someone Else's");
    }

    @Test
    void findByUserIdReturnsEmptyListForUnknownUser() {
        assertThat(portfolioRepository.findByUserId("nobody")).isEmpty();
    }

    @Test
    void findByIdAndUserIdRefusesAnotherUsersPortfolio() {
        Portfolio mine = portfolioRepository.save(new Portfolio("user-1", "Main"));

        assertThat(portfolioRepository.findByIdAndUserId(mine.getId(), "user-1")).isPresent();
        assertThat(portfolioRepository.findByIdAndUserId(mine.getId(), "user-2")).isEmpty();
    }
}
