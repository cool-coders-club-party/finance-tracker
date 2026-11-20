package ie.universityofgalway.finance.portfolio.application.port;

import java.math.BigDecimal;
import ie.universityofgalway.finance.portfolio.domain.model.MarketQuote;
import ie.universityofgalway.finance.portfolio.domain.exception.PriceUnavailableException;

/**
 * Port for obtaining stock prices and, where supported, market quote metadata.
 */
public interface PriceClient {

    /**
     * Obtains the current unit price for trading.
     *
     * @param ticker stock symbol to look up
     * @return the unit price in the instrument's quoted currency
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException if the provider
     *     does not recognise the ticker
     * @throws PriceUnavailableException if the provider cannot supply a usable price
     */
    BigDecimal getPrice(String ticker);

    /**
     * Obtains metadata required to value a holding.
     *
     * <p>The default implementation always fails: a bare price cannot establish currency,
     * timestamp or stale status. Implementations that support valuation must override this method.</p>
     *
     * @param ticker stock symbol to look up
     * @return the price together with currency, timestamp and stale status
     * @throws PriceUnavailableException if metadata is unsupported or the quote is unavailable
     * @throws ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException if the provider
     *     does not recognise the ticker
     */
    default MarketQuote getQuote(String ticker) {
        throw new PriceUnavailableException(
                "Quote metadata unavailable for " + ticker, null);
    }
}
