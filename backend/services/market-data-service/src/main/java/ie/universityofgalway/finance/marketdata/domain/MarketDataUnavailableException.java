package ie.universityofgalway.finance.marketdata.domain;

/**
 * Signals a temporary market-data failure, including quote or history provider failures.
 */
public class MarketDataUnavailableException extends RuntimeException {

    /**
     * Preserves the underlying failure while exposing a stable client-safe message.
     *
     * @param cause provider, transport or response-parsing failure
     */
    public MarketDataUnavailableException(Throwable cause) {
        super("Market data is temporarily unavailable", cause);
    }

}
