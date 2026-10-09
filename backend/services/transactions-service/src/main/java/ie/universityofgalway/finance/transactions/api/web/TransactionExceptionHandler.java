package ie.universityofgalway.finance.transactions.api.web;

import ie.universityofgalway.finance.transactions.application.service.TransactionNotFoundException;
import ie.universityofgalway.finance.transactions.application.service.InvalidTransactionHistoryQueryException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TransactionExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Void> handleTransactionNotFound(TransactionNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(InvalidTransactionHistoryQueryException.class)
    public ResponseEntity<Void> handleInvalidHistoryQuery(InvalidTransactionHistoryQueryException exception) {
        return ResponseEntity.badRequest().build();
    }
}
