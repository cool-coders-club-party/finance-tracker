package ie.universityofgalway.finance.marketdata.infrastructure.twelvedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// The part of /time_series JSON that our service needs.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwelveDataTimeSeriesResponse(
    String status,
    List<Value> values
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(String datetime, String close) {
    }
}