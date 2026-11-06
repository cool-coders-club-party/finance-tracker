package ie.universityofgalway.finance.transactions.util.validator;

import ie.universityofgalway.finance.transactions.currency.FrankfurterRateResponse;
import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import ie.universityofgalway.finance.transactions.exception.ExchangeRateUnavailableException;

import java.time.LocalDate;

public final class ExchangeRateFetchResponseValidator {

    private ExchangeRateFetchResponseValidator() {
    }


    public static void validateExchangeRateResponse(
            TransactionCurrency baseCurrency,
            TransactionCurrency targetCurrency,
            LocalDate date,
            FrankfurterRateResponse response) {
        if (response == null
                || response.date() == null
                || response.date().isAfter(date)
                || !baseCurrency.name().equals(response.base())
                || !targetCurrency.name().equals(response.quote())
                || response.rate() == null
                || response.rate().signum() <= 0) {
            throw new ExchangeRateUnavailableException(
                    "Exchange-rate provider returned an invalid response"
            );
        }
    }

}
