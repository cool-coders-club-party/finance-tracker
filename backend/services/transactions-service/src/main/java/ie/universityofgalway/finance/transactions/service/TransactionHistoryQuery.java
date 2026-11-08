package ie.universityofgalway.finance.transactions.service;

import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionType;

import java.time.LocalDate;
import java.util.UUID;

public record TransactionHistoryQuery(
        UUID userId,
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
