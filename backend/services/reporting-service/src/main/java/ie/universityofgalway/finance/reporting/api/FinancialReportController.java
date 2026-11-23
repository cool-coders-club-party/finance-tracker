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

import ie.universityofgalway.finance.reporting.application.usecase.GenerateFinancialReport;
import ie.universityofgalway.finance.reporting.domain.model.FinancialReport;
import ie.universityofgalway.finance.reporting.api.csv.FinancialReportCsvWriter;

/**
 * Exposes the authenticated financial report CSV export.
 */
@RestController
@RequestMapping("/api/reports")
public class FinancialReportController {

    private final GenerateFinancialReport generateFinancialReport;
    private final FinancialReportCsvWriter csvWriter;

    /**
     * Creates the controller with its report generator and CSV writer.
     *
     * @param generateFinancialReport use case that assembles the report data
     * @param csvWriter writer that encodes the report as CSV
     */
    public FinancialReportController(
            GenerateFinancialReport generateFinancialReport,
            FinancialReportCsvWriter csvWriter) {
        this.generateFinancialReport = generateFinancialReport;
        this.csvWriter = csvWriter;
    }

    /**
     * Downloads a financial report from {@code GET /api/reports/export}.
     *
     * <p>The date range applies to transactions. The investment section contains
     * the caller's current holdings with a positive quantity.</p>
     *
     * @param jwt authenticated caller's JWT, with the user ID as its subject
     * @param from first transaction date to include, in ISO date format
     * @param to last transaction date to include, in ISO date format
     * @return HTTP 200 with a UTF-8 CSV attachment named for the requested date range
     * @throws ResponseStatusException with HTTP 400 if {@code from} is after {@code to}
     * @throws IllegalArgumentException if the JWT subject cannot be parsed as a UUID
     */
    @GetMapping(value = "/export", produces = "text/csv")
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
        FinancialReport report = generateFinancialReport.generate(
                userId, from, to, jwt.getTokenValue());

        byte[] csv = csvWriter.write(report);
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
