package ie.universityofgalway.finance.portfolio.price;

import java.math.BigDecimal;

public interface PriceClient {

    /**
     * @throws ie.universityofgalway.finance.portfolio.exception.InvalidSymbolException
     *         if the ticker is not recognised
     */
    BigDecimal getPrice(String ticker);
}
