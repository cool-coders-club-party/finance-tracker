package ie.universityofgalway.finance.transactions.rest.dto;

import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import ie.universityofgalway.finance.transactions.entity.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Partial update input: null fields preserve existing values and at least one field
 * must be supplied. An empty description clears its text. Type/category compatibility
 * is checked against the resulting entity in the service, not this DTO alone.
 */
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
