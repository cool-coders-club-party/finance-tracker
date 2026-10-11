package ie.universityofgalway.finance.transactions.util.validator;

import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionType;
import ie.universityofgalway.finance.transactions.exception.InvalidTransactionCombinationException;
import org.springframework.stereotype.Component;

@Component
public class TransactionCategoryValidator {

    public void validate(TransactionType type, TransactionCategory category) {
        boolean isAllowed = category.allows(type);
        if (!isAllowed) throw new InvalidTransactionCombinationException(type, category);
    }

}
