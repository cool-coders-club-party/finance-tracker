package ie.universityofgalway.finance.transactions.rest.dto;

import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import ie.universityofgalway.finance.transactions.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDto(
        Long id,
        UUID userId,
        TransactionType transactionType,
        TransactionCategory transactionCategory,
        TransactionCurrency currency,
        BigDecimal amount,
        String description,
        LocalDateTime occurredAt,
        LocalDateTime createdAt
) {
}
