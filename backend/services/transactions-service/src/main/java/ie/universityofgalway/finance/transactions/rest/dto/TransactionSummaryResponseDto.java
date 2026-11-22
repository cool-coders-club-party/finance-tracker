package ie.universityofgalway.finance.transactions.rest.dto;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;

import java.math.BigDecimal;
import java.util.List;

/**
 * Converted totals and category breakdowns for an owner's matching active transactions.
 * Original transaction amounts and currencies remain unchanged.
 *
 * @param currency currency shared by all amounts in this response
 * @param income sum of rounded income category amounts
 * @param expenses sum of rounded expense category amounts
 * @param netIncome income minus expenses; may be negative
 * @param spendingByCategory expense breakdown, empty when no expenses match
 * @param incomeByCategory income breakdown, empty when no income matches
 */
public record TransactionSummaryResponseDto(
        TransactionCurrency currency,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal netIncome,
        List<CategorySpendingResponseDto> spendingByCategory,
        List<CategoryIncomeResponseDto> incomeByCategory
) {
}
