package ie.universityofgalway.finance.portfolio.domain.exception;

import java.math.BigDecimal;

/**
 * Thrown when a sale asks for more shares than the holding contains, including selling a
 * stock the portfolio doesn't hold. Returned to the client as {@code 409 INSUFFICIENT_SHARES}.
 */
public class InsufficientSharesException extends RuntimeException {

    /**
     * @param ticker    the stock symbol
     * @param requested the number of shares the caller tried to sell
     * @param available the number of shares actually held
     */
    public InsufficientSharesException(String ticker, BigDecimal requested, BigDecimal available) {
        super(String.format("Cannot sell %s shares of %s: only %s available", requested, ticker, available));
    }
}
