package ie.universityofgalway.finance.transactions.currency;


import java.math.BigDecimal;
import java.time.LocalDate;

public record FrankfurterRateResponse(
        LocalDate date,
        String base,
        String quote,
        BigDecimal rate
) {
}
