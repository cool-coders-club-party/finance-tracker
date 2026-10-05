package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//DTO

@JsonIgnoreProperties(ignoreUnknown = true)
public record TwelveDataQuoteResponse (
    String symbol,
    String currency,
    String close, //TwelveData sends prices as JSON strings - we'll convert safely to BigDecimal
    Long timestamp
) {
}
