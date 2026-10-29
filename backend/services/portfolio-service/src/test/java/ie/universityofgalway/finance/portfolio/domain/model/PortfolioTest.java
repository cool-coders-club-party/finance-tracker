package ie.universityofgalway.finance.portfolio.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortfolioTest {

    @Test
    void createTrimsTheNameAndHasNoIdYet() {
        Portfolio portfolio = Portfolio.create("user-1", "  Retirement ");

        assertThat(portfolio.getName()).isEqualTo("Retirement");
        assertThat(portfolio.getUserId()).isEqualTo("user-1");
        assertThat(portfolio.getId()).isNull();
    }

    @Test
    void createRejectsABlankName() {
        assertThatThrownBy(() -> Portfolio.create("user-1", "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Portfolio.create("user-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsAMissingOwner() {
        assertThatThrownBy(() -> Portfolio.create(null, "Main"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Portfolio.create(" ", "Main"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}