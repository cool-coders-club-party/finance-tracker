package ie.universityofgalway.finance.portfolio.domain.exception;

/**
 * Thrown when market data doesn't recognise a ticker symbol.
 * Returned to the client as {@code 400 INVALID_SYMBOL}.
 */
public class InvalidSymbolException extends RuntimeException {

    /**
     * @param ticker the unrecognised symbol, as given
     */
    public InvalidSymbolException(String ticker) {
        super("Unknown or invalid ticker symbol: " + ticker);
    }
}
