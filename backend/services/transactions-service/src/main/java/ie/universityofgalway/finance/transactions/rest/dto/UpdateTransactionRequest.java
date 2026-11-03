package ie.universityofgalway.finance.transactions.rest.dto;

import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import ie.universityofgalway.finance.transactions.entity.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateTransactionRequest(
        @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        TransactionCategory transactionCategory,
        @Size(max = 500) String description,
        TransactionCurrency currency,
        TransactionType transactionType,
        @PastOrPresent LocalDateTime occurredAt
) {
    @AssertTrue(message = "Provide at least one field to update")
    public boolean isAnyFieldProvided() {
        return amount != null || transactionCategory != null || description != null
                || transactionType != null || currency != null || occurredAt != null;
    }
}
