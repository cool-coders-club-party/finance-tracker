package ie.universityofgalway.finance.transactions.exception;

import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionType;

public class InvalidTransactionCombinationException extends RuntimeException {
    public InvalidTransactionCombinationException(TransactionType type, TransactionCategory category) {
        super("Transaction type "+type.name()+" is incompatible with category "+category);
    }
}
