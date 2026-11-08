package ie.universityofgalway.finance.transactions.rest.controller;

import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import ie.universityofgalway.finance.transactions.rest.dto.*;
import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionType;
import ie.universityofgalway.finance.transactions.service.TransactionHistoryQuery;
import ie.universityofgalway.finance.transactions.service.TransactionService;
import ie.universityofgalway.finance.transactions.service.TransactionSummaryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionSummaryService transactionSummaryService;

    @PostMapping
    public ResponseEntity<TransactionResponseDto> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(request, userId));
    }

    @GetMapping
    public ResponseEntity<TransactionPageResponseDto> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        TransactionHistoryQuery query = new TransactionHistoryQuery(
                userId, type, category, from, to, page, size, sortBy, direction);
        return ResponseEntity.ok(transactionService.findHistory(query));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> getTransactionById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @NotNull @Positive Long transactionId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(transactionService.findTransactionById(transactionId, userId));
    }

    @PatchMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> updateTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @NotNull @Positive Long transactionId,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(transactionService.updateTransaction(transactionId, request, userId));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @NotNull @Positive Long transactionId,
            @RequestParam(defaultValue = "false") boolean permanent
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        if (permanent) transactionService.hardDelete(transactionId, userId);
        else transactionService.softDelete(transactionId, userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{transactionId}/restore")
    public ResponseEntity<TransactionResponseDto> restoreSoftDeletedTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @NotNull @Positive Long transactionId
    ){
        UUID userId = UUID.fromString(jwt.getSubject());
        TransactionResponseDto transactionResponseDto =
                transactionService.restoreAndGetSoftDeletedTransaction(
                        transactionId,
                        userId
                );
        return ResponseEntity.ok(transactionResponseDto);
    }

    @GetMapping("/deleted")
    public ResponseEntity<TransactionPageResponseDto> getSoftDeletedTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ){
        UUID userId = UUID.fromString(jwt.getSubject());
        TransactionHistoryQuery query = new TransactionHistoryQuery(
                userId, type, category, from, to, page, size, sortBy, direction);
        return ResponseEntity.ok(transactionService.findSoftDeletedTransactions(query));
    }

    @GetMapping("/metadata")
    public ResponseEntity<TransactionMetadataResponseDto> getMetadata() {
        return ResponseEntity.ok(transactionService.findTransactionMetadata());
    }


    @GetMapping("/summary")
    public ResponseEntity<TransactionSummaryResponseDto> getTransactionSummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("currency") TransactionCurrency transactionCurrency,
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        TransactionSummaryResponseDto transactionSummary = transactionSummaryService.getTransactionSummary(
                userId,
                transactionCurrency,
                from,
                to
        );
        return ResponseEntity.ok(transactionSummary);

    }


}
