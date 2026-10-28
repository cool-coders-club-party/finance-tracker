package ie.universityofgalway.finance.portfolio.domain.exception;

public class InvalidSymbolException extends RuntimeException {
    public InvalidSymbolException(String ticker) {
        super("Unknown or invalid ticker symbol: " + ticker);
    }
}
