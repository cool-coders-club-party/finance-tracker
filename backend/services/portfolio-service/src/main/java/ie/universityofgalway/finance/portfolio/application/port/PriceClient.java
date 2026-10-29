package ie.universityofgalway.finance.portfolio.application.port;

import java.math.BigDecimal;

public interface PriceClient {

    /**
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException
     *         if the ticker is not recognised
     */
    BigDecimal getPrice(String ticker);
}
