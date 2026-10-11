package ie.universityofgalway.finance.transactions.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Long transactionId) {
        super("Transaction not found: " + transactionId);
    }
}
