package ie.universityofgalway.finance.transactions.rest.dto;

import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionType;

import java.util.List;

public record TransactionCategoryMetadataDto(
        TransactionCategory name,
        List<TransactionType> allowedTypes
) {
}
