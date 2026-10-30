package ie.universityofgalway.finance.marketdata.domain;

import java.math.BigDecimal;

// Optional details accompanying a stock price.
public record QuoteDetails(
    String name,
    String currency,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    Long volume,
    BigDecimal previousClose,
    BigDecimal change,
    BigDecimal percentChange,
    BigDecimal fiftyTwoWeekLow,
    BigDecimal fiftyTwoWeekHigh
) {
}