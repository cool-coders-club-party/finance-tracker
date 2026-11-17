package ie.universityofgalway.finance.portfolio.application.port;

import java.math.BigDecimal;
import ie.universityofgalway.finance.portfolio.domain.model.MarketQuote;
import ie.universityofgalway.finance.portfolio.domain.exception.PriceUnavailableException;

public interface PriceClient {

    /**
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException
     *         if the ticker is not recognised
     */
    BigDecimal getPrice(String ticker);

    /** Metadata is required for valuation; a bare price must never imply a currency. */
    default MarketQuote getQuote(String ticker) {
        throw new PriceUnavailableException(
                "Quote metadata unavailable for " + ticker, null);
    }
}
