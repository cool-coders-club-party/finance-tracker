package ie.universityofgalway.finance.reporting.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction data included in the transaction section of a report.
 *
 * @param amount transaction amount in its original currency
 * @param currency currency code supplied by the Transactions service
 * @param category category assigned to the transaction
 * @param occurredAt local date and time when the transaction occurred
 */
public record ReportTransaction(
        BigDecimal amount,
        String currency,
        String category,
        LocalDateTime occurredAt) {
}
