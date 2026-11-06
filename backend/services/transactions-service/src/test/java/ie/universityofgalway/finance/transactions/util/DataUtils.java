package ie.universityofgalway.finance.transactions.util;

import ie.universityofgalway.finance.transactions.entity.Transaction;
import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import ie.universityofgalway.finance.transactions.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class DataUtils {
    public static final LocalDate DAY = LocalDate.of(2025, 3, 10);

    private DataUtils() {}

    public static Transaction groceries() {
        Transaction tx = new Transaction();
        tx.setUserId(42L);
        tx.setTransactionType(TransactionType.EXPENSE);
        tx.setTransactionCategory(TransactionCategory.GROCERIES);
        tx.setCurrency(TransactionCurrency.USD);
        tx.setAmount(new BigDecimal("10.50"));
        tx.setOccurredAt(DAY.atTime(12, 0));
        return tx;
    }
}
