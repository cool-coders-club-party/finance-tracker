package ie.universityofgalway.finance.transactions.currency;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;

import java.time.LocalDate;

public interface ExchangeRateProvider {

    ExchangeRate getRate(
            TransactionCurrency baseCurrency,
            TransactionCurrency targetCurrency,
            LocalDate date
    );

}
