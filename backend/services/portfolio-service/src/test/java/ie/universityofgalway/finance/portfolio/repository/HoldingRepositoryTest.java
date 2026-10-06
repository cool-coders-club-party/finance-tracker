package ie.universityofgalway.finance.portfolio.repository;

import ie.universityofgalway.finance.portfolio.model.Holding;
import ie.universityofgalway.finance.portfolio.model.Portfolio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// @DataJpaTest spins up a real (in-memory H2 by default) database, loads only
// JPA-related beans, and wraps each test in a transaction that's rolled back
// afterwards - so tests don't interfere with each other and need no cleanup.
@DataJpaTest
class HoldingRepositoryTest {

    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private HoldingRepository holdingRepository;

    @Test
    void savedHoldingCanBeFoundByPortfolioIdAndTicker() {
        Portfolio portfolio = portfolioRepository.save(new Portfolio("user-1", "Test Portfolio"));

        Holding holding = new Holding(portfolio, "AAPL");
        holding.setQuantity(new BigDecimal("10"));
        holding.setAvgPurchasePrice(new BigDecimal("192.45"));
        holdingRepository.save(holding);

        Optional<Holding> found = holdingRepository.findByPortfolioIdAndTicker(portfolio.getId(), "AAPL");

        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void lookupForNonExistentTickerReturnsEmpty() {
        Portfolio portfolio = portfolioRepository.save(new Portfolio("user-1", "Test Portfolio"));

        Optional<Holding> found = holdingRepository.findByPortfolioIdAndTicker(portfolio.getId(), "MSFT");

        assertThat(found).isEmpty();
    }

    @Test
    void duplicateTickerInSamePortfolioViolatesUniqueConstraint() {
        Portfolio portfolio = portfolioRepository.save(new Portfolio("user-1", "Test Portfolio"));
        holdingRepository.save(new Holding(portfolio, "AAPL"));

        // IDENTITY-strategy inserts happen immediately on save() (Hibernate needs
        // the generated id back right away), not deferred to a later flush - so
        // the violation is thrown from save() itself, which must be inside the
        // assertion lambda, not called beforehand.
        assertThatThrownBy(() -> holdingRepository.save(new Holding(portfolio, "AAPL")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
