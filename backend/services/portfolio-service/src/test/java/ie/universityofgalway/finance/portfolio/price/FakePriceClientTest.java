package ie.universityofgalway.finance.portfolio.price;

import ie.universityofgalway.finance.portfolio.exception.InvalidSymbolException;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FakePriceClientTest {

    private final FakePriceClient priceClient = new FakePriceClient();

    @Test
    public void returnsKnownPriceForAapl() {
        assertThat(priceClient.getPrice("AAPL")).isEqualByComparingTo("192.45");
    }

    @Test
    public void isCaseInsensitive() {
        assertThat(priceClient.getPrice("aapl")).isEqualByComparingTo("192.45");
    }

    @Test
    public void throwsInvalidSymbolExceptionForUnknownTicker() {
        assertThatThrownBy(() -> priceClient.getPrice("ZZZZ"))
                .isInstanceOf(InvalidSymbolException.class)
                .hasMessageContaining("ZZZZ");
    }
}
