package ie.universityofgalway.finance.reporting.csv;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ie.universityofgalway.finance.reporting.portfolio.PortfolioClient;
import ie.universityofgalway.finance.reporting.transactions.TransactionsClient;

@Component
public class FinancialReportCsvWriter {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public byte[] write(
            UUID userId,
            LocalDate from,
            LocalDate to,
            List<TransactionsClient.Transaction> transactions,
            List<PortfolioClient.HoldingPerformance> investments) {

        StringBuilder csv = new StringBuilder();

        appendRow(csv,
                "User ID - Financial Summary from " + from + " to " + to,
                "",
                "");

        appendRow(csv, "", "", "");
        appendRow(csv, "TRANSACTIONS", "", "");
        appendRow(csv, "AMOUNT", "CURRENCY", "CATEGORY", "DATE & TIME");

        for (TransactionsClient.Transaction transaction : transactions) {
            appendRow(csv,
                    formatAmount(transaction.amount()),
                    transaction.currency(),
                    transaction.transactionCategory(),
                    formatDateTime(transaction.occurredAt()));
        }

        appendRow(csv, "", "", "");
        appendRow(csv, "INVESTMENTS", "", "");
        appendRow(csv, "AMOUNT INVESTED", "CURRENT PRICE", "TICKER");

        for (PortfolioClient.HoldingPerformance investment : investments) {
            appendRow(csv,
                    formatAmount(investment.costBasis()),
                    formatAmount(investment.currentPrice()),
                    investment.ticker());
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
            csv.append('"')
                    .append(value.replace("\"", "\"\""))
                    .append('"');
        }

        csv.append("\r\n");
    }
}