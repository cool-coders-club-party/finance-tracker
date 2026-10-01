package ie.universityofgalway.finance.portfolio.exception;

import java.math.BigDecimal;

public class InsufficientSharesException extends RuntimeException {
    public InsufficientSharesException(String ticker, BigDecimal requested, BigDecimal available) {
        super(String.format("Cannot sell %s shares of %s: only %s available", requested, ticker, available));
    }
}
