package ie.universityofgalway.finance.transactions.exception;

public class InvalidTransactionHistoryQueryException extends RuntimeException {

    public InvalidTransactionHistoryQueryException(String message) {
        super(message);
    }
}
