package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.domain.QuoteDetails;
import java.math.BigDecimal;

/**
 * Optional quote statistics, with price amounts expressed in the supplied currency.
 *
 * @param name instrument name; may be null
 * @param currency provider currency code; may be null
 * @param open daily opening price; may be null
 * @param high daily high price; may be null
 * @param low daily low price; may be null
 * @param volume daily traded volume; may be null
 * @param previousClose previous session's closing price; may be null
 * @param change signed price change from the previous close; may be null
 * @param percentChange signed percentage change, where 8 means 8 percent; may be null
 * @param fiftyTwoWeekLow lowest price in the provider's 52-week range; may be null
 * @param fiftyTwoWeekHigh highest price in the provider's 52-week range; may be null
 */
public record QuoteDetailResponse(
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
    /**
     * Maps optional domain statistics into the public response.
     *
     * @param details quote statistics, or null if unavailable
     * @return the mapped statistics, or null when the input is null
     */
    public static QuoteDetailResponse from(QuoteDetails details) {
        if (details == null) {
            return null;
        }

        return new QuoteDetailResponse(
            details.name(),
            details.currency(),
            details.open(),
            details.high(),
            details.low(),
            details.volume(),
            details.previousClose(),
            details.change(),
            details.percentChange(),
            details.fiftyTwoWeekLow(),
            details.fiftyTwoWeekHigh()
        );
    }
}