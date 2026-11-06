package ie.universityofgalway.finance.transactions.currency;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRate(
        TransactionCurrency baseCurrency,
        TransactionCurrency targetCurrency,
        LocalDate rateDate, //date of the rate used
        BigDecimal rate //how much one unit of the base currency is worth in target currency
) {
}
