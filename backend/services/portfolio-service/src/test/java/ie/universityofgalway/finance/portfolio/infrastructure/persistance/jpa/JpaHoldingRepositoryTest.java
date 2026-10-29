package ie.universityofgalway.finance.portfolio.infrastructure.persistance.jpa;

import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.HoldingEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.entity.PortfolioEntity;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa.JpaHoldingRepository;
import ie.universityofgalway.finance.portfolio.infrastructure.persistence.jpa.JpaPortfolioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class JpaHoldingRepositoryTest {

    @Autowired private JpaPortfolioRepository portfolioRepository;
    @Autowired private JpaHoldingRepository holdingRepository;

    private PortfolioEntity portfolio(String userId, String name) {
        return portfolioRepository.save(new PortfolioEntity(userId, name));
    }

    // quantity and avg price are not-null columns and the entity constructor
    // doesn't set them, so the helper does.
    private HoldingEntity holding(PortfolioEntity portfolio, String ticker) {
        HoldingEntity holding = new HoldingEntity(portfolio, ticker);
        holding.setQuantity(new BigDecimal("10"));
        holding.setAvgPurchasePrice(new BigDecimal("100"));
        return holdingRepository.save(holding);
    }

    @Test
    void savedHoldingCanBeFoundByPortfolioIdAndTicker() {
        PortfolioEntity portfolio = portfolio("user-1", "Main");
        holding(portfolio, "AAPL");

        Optional<HoldingEntity> found =
                holdingRepository.findByPortfolioIdAndTicker(portfolio.getId(), "AAPL");

        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void lookupForNonExistentTickerReturnsEmpty() {
        PortfolioEntity portfolio = portfolio("user-1", "Main");

        assertThat(holdingRepository.findByPortfolioIdAndTicker(portfolio.getId(), "MSFT")).isEmpty();
    }

    @Test
    void findByPortfolioIdReturnsOnlyThatPortfoliosHoldings() {
        PortfolioEntity mine = portfolio("user-1", "Mine");
        PortfolioEntity theirs = portfolio("user-2", "Theirs");
        holding(mine, "AAPL");
        holding(mine, "MSFT");
        holding(theirs, "AAPL");

        assertThat(holdingRepository.findByPortfolioId(mine.getId()))
                .extracting(HoldingEntity::getTicker)
                .containsExactlyInAnyOrder("AAPL", "MSFT");
        assertThat(holdingRepository.findByPortfolioId(theirs.getId()))
                .extracting(HoldingEntity::getTicker)
                .containsExactly("AAPL");
    }

    @Test
    void findByPortfolioIdReturnsEmptyListWhenPortfolioHasNoHoldings() {
        PortfolioEntity empty = portfolio("user-1", "Empty");

        assertThat(holdingRepository.findByPortfolioId(empty.getId())).isEmpty();
    }

    @Test
    void duplicateTickerInSamePortfolioViolatesUniqueConstraint() {
        PortfolioEntity portfolio = portfolio("user-1", "Main");
        holding(portfolio, "AAPL");

        // IDENTITY ids mean the INSERT runs inside save(), so the second save is
        // what throws, and it has to be inside the assertion.
        assertThatThrownBy(() -> holding(portfolio, "AAPL"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
