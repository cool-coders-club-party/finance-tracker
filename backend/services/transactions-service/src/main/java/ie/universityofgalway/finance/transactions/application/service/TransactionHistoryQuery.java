package ie.universityofgalway.finance.transactions.application.service;

import ie.universityofgalway.finance.transactions.domain.TransactionCategory;
import ie.universityofgalway.finance.transactions.domain.TransactionType;

import java.time.LocalDate;

public record TransactionHistoryQuery(
        Long userId,
        TransactionType type,
        TransactionCategory category,
        LocalDate from,
        LocalDate to,
        int page,
        int size,
        String sortBy,
        String direction
) {
}
