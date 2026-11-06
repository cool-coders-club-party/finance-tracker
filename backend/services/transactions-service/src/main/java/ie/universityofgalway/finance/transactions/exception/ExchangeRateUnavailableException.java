package ie.universityofgalway.finance.transactions.exception;

public class ExchangeRateUnavailableException extends RuntimeException {
    public ExchangeRateUnavailableException(String message) {
        super(message);
    }

    public ExchangeRateUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
