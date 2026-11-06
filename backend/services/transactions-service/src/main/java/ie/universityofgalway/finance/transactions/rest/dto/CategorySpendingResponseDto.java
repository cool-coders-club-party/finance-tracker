package ie.universityofgalway.finance.transactions.rest.dto;

import ie.universityofgalway.finance.transactions.entity.TransactionCategory;

import java.math.BigDecimal;

public record CategorySpendingResponseDto(
        TransactionCategory category,
        BigDecimal amount
) {
}
