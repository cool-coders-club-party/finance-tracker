package ie.universityofgalway.finance.transactions.currency.caching;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;

import java.time.LocalDate;

public record ExchangeRateCacheKey(
        TransactionCurrency baseCurrency,
        TransactionCurrency targetCurrency,
        LocalDate requestedDate
) {

    public String toRedisKey() {
        return "transactions:fx:ecb:v1:"
                + baseCurrency.name() + ":"
                + targetCurrency.name() + ":"
                + requestedDate;
    }

}
