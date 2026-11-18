package ie.universityofgalway.finance.reporting.api.csv;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import ie.universityofgalway.finance.reporting.domain.model.FinancialReport;
import ie.universityofgalway.finance.reporting.domain.model.ReportInvestment;
import ie.universityofgalway.finance.reporting.domain.model.ReportTransaction;

@Component
public class FinancialReportCsvWriter {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public byte[] write(FinancialReport report) {
        StringBuilder csv = new StringBuilder();

        appendRow(csv,
                "User ID " + report.userId()
                        + " - Financial Summary from "
                        + report.from() + " to " + report.to(),
                "", "", "");

        appendRow(csv, "", "", "", "");
        appendRow(csv, "TRANSACTIONS", "", "", "");
        appendRow(csv, "AMOUNT", "CURRENCY", "CATEGORY", "DATE & TIME");

        for (ReportTransaction transaction : report.transactions()) {
            appendRow(csv,
                    formatAmount(transaction.amount()),
                    transaction.currency(),
                    transaction.category(),
                    formatDateTime(transaction.occurredAt()));
        }

        appendRow(csv, "", "", "", "");
        appendRow(csv, "INVESTMENTS", "", "", "");
        appendRow(csv, "AMOUNT INVESTED", "CURRENT PRICE", "TICKER", "");

        for (ReportInvestment investment : report.investments()) {
            appendRow(csv,
                    formatAmount(investment.amountInvested()),
                    formatAmount(investment.currentPrice()),
                    investment.ticker(),
                    "");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String formatAmount(BigDecimal amount) {
        return amount == null ? "" : amount.toPlainString();
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMAT.format(dateTime);
    }

    private static void appendRow(StringBuilder csv, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                csv.append(',');
            }

            String value = cells[i] == null ? "" : cells[i];
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }

        csv.append("\r\n");
    }
}