package ie.universityofgalway.finance.transactions.currency;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A conversion rate together with its direction and actual effective date.
 *
 * @param baseCurrency currency being converted from
 * @param targetCurrency currency being converted to
 * @param rateDate effective date supplied by the provider, which may precede the requested date
 * @param rate target-currency units per one unit of base currency
 */
public record ExchangeRate(
        TransactionCurrency baseCurrency,
        TransactionCurrency targetCurrency,
        LocalDate rateDate, //date of the rate used
        BigDecimal rate //how much one unit of the base currency is worth in target currency
) {
}
