package ie.universityofgalway.finance.transactions.api.web;

import ie.universityofgalway.finance.transactions.api.web.dto.CreateTransactionRequest;
import ie.universityofgalway.finance.transactions.api.web.dto.TransactionResponse;
import ie.universityofgalway.finance.transactions.application.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(request));
    }

    @GetMapping
    public List<TransactionResponse> findByUserId(
            @RequestParam @NotNull @Positive Long userId) {
        return transactionService.findByUserId(userId);
    }
}
