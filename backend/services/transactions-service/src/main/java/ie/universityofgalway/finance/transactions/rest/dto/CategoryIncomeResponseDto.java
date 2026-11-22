package ie.universityofgalway.finance.transactions.rest.dto;

import ie.universityofgalway.finance.transactions.entity.TransactionCategory;

import java.math.BigDecimal;

/**
 * Income for one category, expressed in the enclosing summary's currency.
 *
 * @param category income category
 * @param amount converted category total rounded to two decimals using HALF_EVEN
 */
public record CategoryIncomeResponseDto(
        TransactionCategory category,
        BigDecimal amount
) {
}
