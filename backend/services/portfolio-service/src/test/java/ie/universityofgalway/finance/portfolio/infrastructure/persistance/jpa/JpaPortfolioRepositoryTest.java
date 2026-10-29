package ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa;

import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.PortfolioEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JpaPortfolioRepositoryTest {

    @Autowired private JpaPortfolioRepository portfolioRepository;

    @Test
    void findByUserIdReturnsOnlyThatUsersPortfolios() {
        portfolioRepository.save(new PortfolioEntity("user-1", "Main"));
        portfolioRepository.save(new PortfolioEntity("user-1", "Retirement"));
        portfolioRepository.save(new PortfolioEntity("user-2", "Someone Else's"));

        assertThat(portfolioRepository.findByUserId("user-1"))
                .extracting(PortfolioEntity::getName)
                .containsExactlyInAnyOrder("Main", "Retirement");
        assertThat(portfolioRepository.findByUserId("user-2"))
                .extracting(PortfolioEntity::getName)
                .containsExactly("Someone Else's");
    }

    @Test
    void findByUserIdReturnsEmptyListForUnknownUser() {
        assertThat(portfolioRepository.findByUserId("nobody")).isEmpty();
    }

    @Test
    void findByIdAndUserIdRefusesAnotherUsersPortfolio() {
        PortfolioEntity mine = portfolioRepository.save(new PortfolioEntity("user-1", "Main"));

        assertThat(portfolioRepository.findByIdAndUserId(mine.getId(), "user-1")).isPresent();
        assertThat(portfolioRepository.findByIdAndUserId(mine.getId(), "user-2")).isEmpty();
    }
}