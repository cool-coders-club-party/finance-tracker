package ie.universityofgalway.finance.transactions.entity;

import java.util.EnumSet;

public enum TransactionCategory {
    SALARY(TransactionType.INCOME),
    FREELANCE(TransactionType.INCOME),
    INVESTMENT_RETURN(TransactionType.INCOME),
    GIFT(TransactionType.INCOME, TransactionType.EXPENSE),
    REFUND(TransactionType.INCOME),
    HOUSING(TransactionType.EXPENSE),
    GROCERIES(TransactionType.EXPENSE),
    DINING(TransactionType.EXPENSE),
    TRANSPORT(TransactionType.EXPENSE),
    UTILITIES(TransactionType.EXPENSE),
    HEALTHCARE(TransactionType.EXPENSE),
    ENTERTAINMENT(TransactionType.EXPENSE),
    SHOPPING(TransactionType.EXPENSE),
    EDUCATION(TransactionType.EXPENSE),
    TRAVEL(TransactionType.EXPENSE),
    SUBSCRIPTIONS(TransactionType.EXPENSE),
    INSURANCE(TransactionType.EXPENSE),
    DEBT_REPAYMENT(TransactionType.EXPENSE),
    OTHER(TransactionType.INCOME, TransactionType.EXPENSE);

    private final EnumSet<TransactionType> allowedTypes;

    TransactionCategory(TransactionType firstAllowedType, TransactionType... additionalAllowedTypes) {
        this.allowedTypes = EnumSet.of(firstAllowedType, additionalAllowedTypes);
    }

    public boolean allows(TransactionType type) {
        return allowedTypes.contains(type);
    }
}
