package ie.universityofgalway.finance.transactions.api.web.dto;

import ie.universityofgalway.finance.transactions.domain.TransactionCategory;
import ie.universityofgalway.finance.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDto(
        Long id,
        Long userId,
        TransactionType transactionType,
        TransactionCategory transactionCategory,
        BigDecimal amount,
        String description,
        LocalDateTime occurredAt,
        LocalDateTime createdAt
) {
}
