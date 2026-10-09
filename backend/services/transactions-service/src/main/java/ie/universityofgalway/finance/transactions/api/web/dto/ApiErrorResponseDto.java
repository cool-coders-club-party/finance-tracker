package ie.universityofgalway.finance.transactions.api.web.dto;

import java.util.Map;

public record ApiErrorResponseDto(
        int status,
        String error,
        String message,
        String path,
        Map<String, String> details
) {
}
