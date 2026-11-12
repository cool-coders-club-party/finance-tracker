package ie.universityofgalway.finance.reporting.transactions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TransactionsReportService {

    private final TransactionsClient transactionsClient;

    public TransactionsReportService(TransactionsClient transactionsClient) {
        this.transactionsClient = transactionsClient;
    }

    public List<TransactionsClient.Transaction> getTransactions(
            LocalDate from,
            LocalDate to,
            String jwtToken) {

        List<TransactionsClient.Transaction> transactions = new ArrayList<>();
        int page = 0;
        int totalPages;

        do {
            TransactionsClient.TransactionPage result =
                    transactionsClient.getPage(from, to, page, jwtToken);

            transactions.addAll(result.content());
            totalPages = result.totalPages();
            page++;
        } while (page < totalPages);

        return List.copyOf(transactions);
    }
}