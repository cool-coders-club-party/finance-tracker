package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Provider-specific daily time-series response; unknown JSON properties are ignored.
 *
 * @param status provider status, including the value error for a rejected request
 * @param values daily observations in provider order; may be null for invalid responses
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwelveDataTimeSeriesResponse(
    String status,
    List<Value> values
) {
    /**
     * One daily observation before domain conversion.
     *
     * @param datetime trading date as ISO local-date text
     * @param close closing price as decimal text
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(String datetime, String close) {
    }
}