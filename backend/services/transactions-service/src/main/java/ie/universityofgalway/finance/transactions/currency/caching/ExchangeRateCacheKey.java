package ie.universityofgalway.finance.transactions.currency.caching;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;

import java.time.LocalDate;

/**
 * Identifies a directional ECB rate lookup by its requested date, not its effective date.
 * The Redis prefix includes the provider and format version to isolate incompatible entries.
 *
 * @param baseCurrency currency being converted from
 * @param targetCurrency currency being converted to
 * @param requestedDate lookup date, including days whose rate may come from an earlier date
 */
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
