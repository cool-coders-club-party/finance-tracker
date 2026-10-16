package ie.universityofgalway.finance.transactions.rest.controller;

import ie.universityofgalway.finance.transactions.rest.dto.*;
import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionType;
import ie.universityofgalway.finance.transactions.service.TransactionHistoryQuery;
import ie.universityofgalway.finance.transactions.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponseDto> create(
            @Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(request));
    }

    @GetMapping
    public ResponseEntity<TransactionPageResponseDto> getHistory(
            @RequestParam @NotNull @Positive Long userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        TransactionHistoryQuery query = new TransactionHistoryQuery(
                userId, type, category, from, to, page, size, sortBy, direction);
        return ResponseEntity.ok(transactionService.findHistory(query));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> findTransactionById(
            @PathVariable @NotNull @Positive Long transactionId) {
        return ResponseEntity.ok(transactionService.findTransactionById(transactionId));
    }

    @PatchMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> updateTransaction(
            @PathVariable @NotNull @Positive Long transactionId,
            @Valid @RequestBody UpdateTransactionRequest request) {
        return ResponseEntity.ok(transactionService.updateTransaction(transactionId, request));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable @NotNull @Positive Long transactionId,
            @RequestParam(defaultValue = "false") boolean permanent
    ) {
        if (permanent) transactionService.hardDelete(transactionId);
        else transactionService.softDelete(transactionId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/metadata")
    public ResponseEntity<TransactionMetadataResponseDto> getMetadata(){
        return ResponseEntity.ok(transactionService.findTransactionMetadata());
    }


}
