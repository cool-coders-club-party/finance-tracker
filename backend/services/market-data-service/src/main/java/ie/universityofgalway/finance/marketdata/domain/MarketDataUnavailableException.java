package ie.universityofgalway.finance.marketdata.domain;

//signals that neither a live quote nor a cached fallback is available
public class MarketDataUnavailableException extends RuntimeException {

    public MarketDataUnavailableException(Throwable cause) {
        //super() calls a constructor in the superclass RuntimeException
        super("Market data is temporarily unavailable", cause);
    }

}
