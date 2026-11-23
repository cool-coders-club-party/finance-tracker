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

// Handling web requests.
@RestController
// Shared URL prefix.
@RequestMapping("/api/reports")
public class FinancialReportController {

    private final GenerateFinancialReport generateFinancialReport;
    private final FinancialReportCsvWriter csvWriter;

    public FinancialReportController(
            GenerateFinancialReport generateFinancialReport,
            FinancialReportCsvWriter csvWriter) {
        this.generateFinancialReport = generateFinancialReport;
        this.csvWriter = csvWriter;
    }
    // Map this method to GET /api/reports/export and declare that it returns a CSV.
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            // Provide JWT.
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam
            // Parse as dates.
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // Reject range date where the start is after the end.
        if (from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "from must be on or before to");
        }
        // Convert to UUID, which is used by the application. generateFinancialReport
        // asks the app use case to collect the report data.
        UUID userId = UUID.fromString(jwt.getSubject());
        FinancialReport report = generateFinancialReport.generate(
                userId, from, to, jwt.getTokenValue());
        // Convert report into CSV bytes.
        byte[] csv = csvWriter.write(report);
        String filename = "financial-summary-" + from + "-to-" + to + ".csv";
        // Create a HTTP 200 response, set the CSV content type and an attachment header with the filename, put CSV bytes in response body.
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