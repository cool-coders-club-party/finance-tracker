package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.domain.QuoteDetails;
import java.math.BigDecimal;

// Optional statistics shown with a quote.
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