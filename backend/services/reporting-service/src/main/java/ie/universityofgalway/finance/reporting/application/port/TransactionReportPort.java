package ie.universityofgalway.finance.reporting.application.port;

import java.time.LocalDate;
import java.util.List;

import ie.universityofgalway.finance.reporting.domain.model.ReportTransaction;

public interface TransactionReportPort {

    List<ReportTransaction> getTransactions(
            LocalDate from,
            LocalDate to,
            String accessToken);
}