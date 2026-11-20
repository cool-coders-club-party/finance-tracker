package ie.universityofgalway.finance.transactions.currency;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Converts amounts through the provider boundary without changing stored transactions.
 */
@Service
@RequiredArgsConstructor
public class CurrencyConversionService {
    private final ExchangeRateProvider exchangeRateProvider;

    /**
     * Multiplies an amount by the historical rate without rounding. Callers aggregate
     * these results and choose the rounding policy appropriate to their output.
     *
     * @param amount original amount, non-null
     * @param sourceCurrency original currency, non-null
     * @param targetCurrency desired currency, non-null
     * @param date date used to select the rate, non-null
     * @return the unrounded converted amount
     * @throws NullPointerException if an argument is null
     * @throws ie.universityofgalway.finance.transactions.exception.ExchangeRateUnavailableException if a required rate is unavailable
     */
    public BigDecimal convert(
            BigDecimal amount,
            TransactionCurrency sourceCurrency,
            TransactionCurrency targetCurrency,
            LocalDate date
    ) {

        Objects.requireNonNull(sourceCurrency, "sourceCurrency must be not null");
        Objects.requireNonNull(targetCurrency, "targetCurrency must be not null");
        Objects.requireNonNull(amount, "amount must be not null");
        Objects.requireNonNull(date, "date must be not null");
        ExchangeRate rate = exchangeRateProvider.getRate(sourceCurrency, targetCurrency, date);
        return amount.multiply(rate.rate());
    }
}

