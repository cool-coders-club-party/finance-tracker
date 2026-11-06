package ie.universityofgalway.finance.transactions.rest.dto;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;

import java.math.BigDecimal;
import java.util.List;

public record TransactionSummaryResponseDto(
        TransactionCurrency currency,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal netIncome,
        List<CategorySpendingResponseDto> spendingByCategory
) {
}
