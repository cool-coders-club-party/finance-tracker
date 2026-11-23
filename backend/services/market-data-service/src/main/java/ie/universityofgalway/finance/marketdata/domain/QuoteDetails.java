package ie.universityofgalway.finance.marketdata.domain;

import java.math.BigDecimal;

/**
 * Optional instrument metadata and daily statistics supplied with a quote.
 *
 * <p>All fields may be null. Price amounts use the supplied currency.</p>
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