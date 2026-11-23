package ie.universityofgalway.finance.transactions.currency;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;

import java.time.LocalDate;

/**
 * Boundary for obtaining historical conversion rates independently of HTTP and caching.
 */
public interface ExchangeRateProvider {

    /**
     * Returns target-currency units per one base-currency unit for a requested date.
     * The effective rate date may precede the request date, but must not follow it.
     * Identical currencies yield a rate of one without an external rate lookup.
     *
     * @param baseCurrency original currency, non-null
     * @param targetCurrency desired currency, non-null
     * @param date requested historical date, non-null
     * @return a positive rate with its actual effective date
     * @throws ie.universityofgalway.finance.transactions.exception.ExchangeRateUnavailableException if no valid rate can be obtained
     */
    ExchangeRate getRate(
            TransactionCurrency baseCurrency,
            TransactionCurrency targetCurrency,
            LocalDate date
    );

}
