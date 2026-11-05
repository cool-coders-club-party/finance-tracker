package ie.universityofgalway.finance.portfolio.domain.exception;

public class PriceUnavailableException  extends RuntimeException{
    public PriceUnavailableException(String message,  Throwable cause){
        super(message,cause);
    }
}
