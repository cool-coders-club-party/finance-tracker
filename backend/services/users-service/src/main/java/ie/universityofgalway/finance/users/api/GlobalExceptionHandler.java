package ie.universityofgalway.finance.users.api;

import ie.universityofgalway.finance.users.domain.EmailAlreadyRegisteredException;
import jakarta.validation.constraints.Email;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Converts exceptions thrown in the controllers into HTTP responses
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, Object>> handleEmailTaken(EmailAlreadyRegisteredException e){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", 409, "message", e.getMessage()));
    }
}
