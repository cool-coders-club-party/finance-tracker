package ie.universityofgalway.finance.transactions.api.web.dto;

import ie.universityofgalway.finance.transactions.domain.TransactionCategory;
import ie.universityofgalway.finance.transactions.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateTransactionRequest(
        @NotNull @Positive Long userId,
        @NotNull TransactionType transactionType,
        @NotNull TransactionCategory transactionCategory,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @Size(max = 500) String description,
        @NotNull @PastOrPresent LocalDateTime occurredAt
) {
}
