package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Fields we use from Twelve Data's /quote response.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwelveDataQuoteResponse(
    String symbol,
    String name,
    String currency,
    String close,
    Long timestamp,
    String open,
    String high,
    String low,
    String volume,
    @JsonProperty("previous_close") String previousClose,
    String change,
    @JsonProperty("percent_change") String percentChange,
    @JsonProperty("fifty_two_week") FiftyTwoWeek fiftyTwoWeek
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FiftyTwoWeek(String low, String high) {
    }
}