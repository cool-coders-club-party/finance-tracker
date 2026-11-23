package ie.universityofgalway.finance.portfolio.domain.exception;

/**
 * Thrown when a price can't be obtained: market data is down, timed out, or returned an
 * unusable quote. Not the user's fault, and usually temporary.
 *
 * <p>Buy, sell and performance requests fail with {@code 503 PRICE_UNAVAILABLE}. Valuation
 * instead marks the affected holding as unavailable and carries on.</p>
 */
public class PriceUnavailableException  extends RuntimeException{

    /**
     * @param message what went wrong, e.g. {@code "Could not get a price for AAPL from Market Data"}
     * @param cause   the underlying error, or {@code null}
     */
    public PriceUnavailableException(String message,  Throwable cause){
        super(message,cause);
    }
}
