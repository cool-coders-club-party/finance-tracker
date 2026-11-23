package ie.universityofgalway.finance.portfolio.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes the response when a request has no valid access token: missing, malformed,
 * badly signed or expired.
 *
 * <p>Returns {@code 401 Unauthorized} with a {@code WWW-Authenticate: Bearer} header and the
 * JSON body {@code {"code": "UNAUTHORIZED", "message": ...}}, so clients get JSON instead of
 * Spring's default empty response. Note that this uses {@code code} where
 * {@link ie.universityofgalway.finance.portfolio.api.dto.ErrorResponse} uses {@code error}.</p>
 *
 * <p>Registered in {@link SecurityConfig}.</p>
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String BODY =
            "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication is required to access this resource\"}";

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(BODY);
    }
}