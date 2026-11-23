package ie.universityofgalway.finance.reporting.domain.model;

import java.math.BigDecimal;

/**
 * Current portfolio holding included in the investment section of a report.
 *
 * @param ticker trading symbol of the holding
 * @param quantity number of units held
 * @param amountInvested cost basis supplied by the Portfolio service
 * @param currentPrice current price per unit supplied by the Portfolio service
 */
public record ReportInvestment(
        String ticker,
        BigDecimal quantity,
        BigDecimal amountInvested,
        BigDecimal currentPrice) {
}
