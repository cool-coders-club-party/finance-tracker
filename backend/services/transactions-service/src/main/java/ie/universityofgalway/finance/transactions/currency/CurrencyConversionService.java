package ie.universityofgalway.finance.transactions.currency;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CurrencyConversionService {
    private final ExchangeRateProvider exchangeRateProvider;

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

