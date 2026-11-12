package ie.universityofgalway.finance.reporting.api;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ie.universityofgalway.finance.reporting.csv.FinancialReportCsvWriter;
import ie.universityofgalway.finance.reporting.portfolio.PortfolioReportService;
import ie.universityofgalway.finance.reporting.transactions.TransactionsReportService;

@RestController
@RequestMapping("/api/reports")
public class FinancialReportController {

    private final TransactionsReportService transactionsReportService;
    private final PortfolioReportService portfolioReportService;
    private final FinancialReportCsvWriter csvWriter;

    public FinancialReportController(
            TransactionsReportService transactionsReportService,
            PortfolioReportService portfolioReportService,
            FinancialReportCsvWriter csvWriter) {
        this.transactionsReportService = transactionsReportService;
        this.portfolioReportService = portfolioReportService;
        this.csvWriter = csvWriter;
    }

    @GetMapping(
            value = "/export",
            produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "from must be on or before to");
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        String jwtToken = jwt.getTokenValue();

        var transactions =
                transactionsReportService.getTransactions(from, to, jwtToken);
        var investments =
                portfolioReportService.getInvestments(jwtToken);

        byte[] csv = csvWriter.write(
                userId, from, to, transactions, investments);

        String filename = "financial-summary-" + from + "-to-" + to + ".csv";

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString())
                .body(csv);
    }
}