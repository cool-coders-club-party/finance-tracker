package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Provider-specific fields read from Twelve Data's quote endpoint.
 *
 * <p>Numeric strings are converted by the provider adapter; omitted fields remain null.</p>
 *
 * @param symbol provider ticker
 * @param name instrument name
 * @param currency price currency code
 * @param close latest price as decimal text
 * @param timestamp quote time as Unix epoch seconds
 * @param open daily opening price as text
 * @param high daily high as text
 * @param low daily low as text
 * @param volume daily volume as integer text
 * @param previousClose previous session close as text
 * @param change signed daily price change as text
 * @param percentChange signed daily percentage change as text
 * @param fiftyTwoWeek optional 52-week price range
 */
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
    /**
     * Provider-specific 52-week price range.
     *
     * @param low lowest price as decimal text; may be null
     * @param high highest price as decimal text; may be null
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FiftyTwoWeek(String low, String high) {
    }
}