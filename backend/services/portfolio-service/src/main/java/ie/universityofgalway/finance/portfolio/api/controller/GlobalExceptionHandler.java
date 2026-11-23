package ie.universityofgalway.finance.portfolio.api.controller;

import ie.universityofgalway.finance.portfolio.api.dto.ErrorResponse;
import ie.universityofgalway.finance.portfolio.domain.exception.InsufficientSharesException;
import ie.universityofgalway.finance.portfolio.domain.exception.InvalidSymbolException;
import ie.universityofgalway.finance.portfolio.domain.exception.PortfolioNotFoundException;
import ie.universityofgalway.finance.portfolio.domain.exception.PriceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions thrown by any controller into JSON error responses.
 *
 * <p>Every response has the {@link ErrorResponse} shape {@code {"error": CODE, "message": text}},
 * so the frontend can show the message and branch on the code. Controllers and services just
 * throw; none of them build error responses themselves.</p>
 *
 * <p>Authentication failures never reach this class: Spring Security rejects them earlier,
 * through {@link ie.universityofgalway.finance.portfolio.infrastructure.config.JsonAuthenticationEntryPoint}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Market data does not recognise the ticker.
     *
     * @param ex the exception naming the ticker
     * @return {@code 400 Bad Request} with code {@code INVALID_SYMBOL}
     */
    @ExceptionHandler(InvalidSymbolException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSymbol(InvalidSymbolException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_SYMBOL", ex.getMessage()));
    }

    /**
     * A sale asked for more shares than the holding contains.
     *
     * @param ex the exception with the requested and available quantities
     * @return {@code 409 Conflict} with code {@code INSUFFICIENT_SHARES}
     */
    @ExceptionHandler(InsufficientSharesException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientShares(InsufficientSharesException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("INSUFFICIENT_SHARES", ex.getMessage()));
    }

    /**
     * The portfolio doesn't exist, or belongs to another user. Both cases look the same,
     * so the API never reveals which portfolio ids exist.
     *
     * @param ex the exception naming the portfolio id
     * @return {@code 404 Not Found} with code {@code NOT_FOUND}
     */
    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PortfolioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    /**
     * A business rule was broken, for example a backdated purchase dated in the future,
     * or {@code price} sent without {@code executedAt}.
     *
     * @param ex the exception whose message explains the rule
     * @return {@code 400 Bad Request} with code {@code VALIDATION_ERROR}
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleDomainRule(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }

    /**
     * A request body failed its Bean Validation annotations (see {@code TradeRequest} and
     * {@code CreatePortfolioRequest}). Only the first failing field is reported.
     *
     * @param ex the validation result from Spring
     * @return {@code 400 Bad Request} with code {@code VALIDATION_ERROR} and a message such as
     *         {@code "quantity: Quantity must be greater than zero"}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    /**
     * Market data was unreachable or returned an unusable quote, so a trade or performance
     * request couldn't be priced. This is not the user's fault and is usually temporary.
     *
     * @param ex the exception describing the failure
     * @return {@code 503 Service Unavailable} with code {@code PRICE_UNAVAILABLE}
     */
    @ExceptionHandler(PriceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handlePriceUnavailable(PriceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("PRICE_UNAVAILABLE", ex.getMessage()));
    }
}