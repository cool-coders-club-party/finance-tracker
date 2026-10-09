package ie.universityofgalway.finance.transactions.application.service;

public class InvalidTransactionHistoryQueryException extends RuntimeException {

    public InvalidTransactionHistoryQueryException(String message) {
        super(message);
    }
}
