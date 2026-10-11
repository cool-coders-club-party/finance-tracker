package ie.universityofgalway.finance.portfolio.price;

import ie.universityofgalway.finance.portfolio.exception.InvalidSymbolException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakePriceClientTest {

    private final FakePriceClient priceClient = new FakePriceClient();

    @Test
    void returnsKnownPriceForAapl() {
        assertThat(priceClient.getPrice("AAPL")).isEqualByComparingTo("192.45");
    }

    @Test
    void isCaseInsensitive() {
        assertThat(priceClient.getPrice("aapl")).isEqualByComparingTo("192.45");
    }

    @Test
    void throwsInvalidSymbolExceptionForUnknownTicker() {
        assertThatThrownBy(() -> priceClient.getPrice("ZZZZ"))
                .isInstanceOf(InvalidSymbolException.class)
                .hasMessageContaining("ZZZZ");
    }
}
