package ie.universityofgalway.finance.transactions.api.web.dto;

import ie.universityofgalway.finance.transactions.domain.Transaction;
import ie.universityofgalway.finance.transactions.domain.TransactionCategory;
import ie.universityofgalway.finance.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long userId,
        TransactionType transactionType,
        TransactionCategory transactionCategory,
        BigDecimal amount,
        String description,
        LocalDateTime occurredAt,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getTransactionType(),
                transaction.getTransactionCategory(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getOccurredAt(),
                transaction.getCreatedAt()
        );
    }
}
