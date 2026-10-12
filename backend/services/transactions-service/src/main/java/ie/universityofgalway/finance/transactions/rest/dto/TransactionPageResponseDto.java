package ie.universityofgalway.finance.transactions.rest.dto;

import java.util.List;

public record TransactionPageResponseDto(
        List<TransactionResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
