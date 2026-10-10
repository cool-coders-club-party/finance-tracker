package ie.universityofgalway.finance.transactions.exception;

import ie.universityofgalway.finance.transactions.rest.dto.ApiErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class TransactionExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDto> handleTransactionNotFound(
            TransactionNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidTransactionHistoryQueryException.class)
    public ResponseEntity<ApiErrorResponseDto> handleInvalidHistoryQuery(
            InvalidTransactionHistoryQueryException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDto> handleInvalidBody(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        addErrors(details, exception.getBindingResult().getFieldErrors(),
                exception.getBindingResult().getGlobalErrors());
        return error(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler(InvalidTransactionCombinationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleInvalidTransactionCombination(
            InvalidTransactionCombinationException e, HttpServletRequest request
    ){
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleInvalidParameters(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                addErrors(details, parameterErrors.getFieldErrors(), parameterErrors.getGlobalErrors());
            } else {
                String name = result.getMethodParameter().getParameterName();
                String parameter = name != null ? name : "parameter";
                result.getResolvableErrors().forEach(issue ->
                        details.putIfAbsent(parameter, message(issue.getDefaultMessage())));
            }
        }
        return error(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponseDto> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof InvalidFormatException invalidFormat
                    && invalidFormat.getTargetType() != null
                    && invalidFormat.getTargetType().isEnum()) {
                String field = invalidFormat.getPath().stream()
                        .map(JacksonException.Reference::getPropertyName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("."));
                return error(HttpStatus.BAD_REQUEST, "Invalid enum value", request,
                        Map.of(field.isEmpty() ? "value" : field, allowedValues(invalidFormat.getTargetType())));
            }
            cause = cause.getCause();
        }
        return error(HttpStatus.BAD_REQUEST, "Malformed JSON or invalid field value", request, Map.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Missing required request parameter", request,
                Map.of(exception.getParameterName(), "is required"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponseDto> handleInvalidParameterType(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        Class<?> requiredType = exception.getRequiredType();
        String detail = requiredType != null && requiredType.isEnum()
                ? allowedValues(requiredType) : "has an invalid value";
        return error(HttpStatus.BAD_REQUEST, "Invalid request parameter", request,
                Map.of(exception.getName(), detail));
    }

    private void addErrors(Map<String, String> details,
                           Iterable<FieldError> fieldErrors, Iterable<ObjectError> globalErrors) {
        for (FieldError fieldError : fieldErrors) {
            details.putIfAbsent(fieldError.getField(), message(fieldError.getDefaultMessage()));
        }
        for (ObjectError globalError : globalErrors) {
            details.putIfAbsent("request", message(globalError.getDefaultMessage()));
        }
    }

    private String message(String defaultMessage) {
        return defaultMessage != null ? defaultMessage : "Invalid value";
    }

    private String allowedValues(Class<?> enumType) {
        return "must be one of: " + Arrays.stream(enumType.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

    private ResponseEntity<ApiErrorResponseDto> error(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> details) {
        return ResponseEntity.status(status).body(new ApiErrorResponseDto(
                status.value(), status.name(), message, request.getRequestURI(), details));
    }
}
