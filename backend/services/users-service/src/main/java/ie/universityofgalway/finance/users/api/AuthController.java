package ie.universityofgalway.finance.users.api;

import ie.universityofgalway.finance.users.api.dto.LoginRequest;
import ie.universityofgalway.finance.users.api.dto.RefreshTokenRequest;
import ie.universityofgalway.finance.users.api.dto.RegisterRequest; // pull in DTO
import ie.universityofgalway.finance.users.application.*;
import ie.universityofgalway.finance.users.domain.User;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping; // mapping GETs
import org.springframework.web.bind.annotation.PostMapping; // mapping POSTs
import org.springframework.web.bind.annotation.RequestMapping; // declaring endpoint prefixes
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for the authentication endpoints of the users service.
 *
 * <p>All endpoints are served under the {@code /api/auth} prefix:
 * <ul>
 *   <li>{@code GET /api/auth/ping} - simple liveness check</li>
 *   <li>{@code POST /api/auth/register} - create a new account, receive an access token and a refresh token</li>
 *   <li>{@code POST /api/auth/login} - authenticate credentials, and receive an access token</li>
 *   <li>{@code GET /api/auth/me} - return identity of authenticated caller</li>
 *   <li>{@code POST /api/auth/refresh} - exchange a valid refresh token for a new access and refresh token</li>
 *   <li>{@code POST /api/auth/logout} - revoke a refresh token, requiring the user to login to use the service again</li>
 * </ul>
 *
 * <p>This class only translates between HTTP and Java: it receives requests, triggers
 * validation and builds responses. Business rules belong in the application layer.
 * ({@link AuthService}, {@link RefreshTokenService} and {@link TokenIssuer})
 */
@RestController // tells Spring this class handles web requests and that method returns are the response body
@RequestMapping("/api/auth") // path prefix for the class, every endpoint starts with /api/auth
public class AuthController {

    private final AuthService authService;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, TokenIssuer tokenIssuer, RefreshTokenService refreshTokenService){
        this.authService = authService;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Minimal liveness check for the auth API
     *
     * @return plaintext message {@code "auth api is working"} with status {@code 200 OK}
     */
    @GetMapping("/ping") // maps GET requests for /ping to this method and declares the endpoint GET /api/auth/ping (prefix + /ping)
    public String ping(){ // method is a handler for that endpoint that runs when it is called
        return "auth api is working"; // sent back as the plaintext response body with status 200 OK
    }

    /**
     * Registers a new user account.
     *
     * <p>The request body is validated against the constraints on {@link RegisterRequest}
     * before this method runs; if validation fails, Spring responds with
     * {@code 400 Bad Request} and this method is never called.
     *
     * <p> Registration itself it handled by {@link AuthService#register(String, String)}.</p>
     *
     * <p> {@code @Valid}
     *     {@code @RequestBody}
     *
     *
     * @param request the validated email and password submitted by the client
     * @return {@code 201 Created} with a JSON body containing the user's new {@code id} and {@code email}
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@Valid @RequestBody RegisterRequest request){
        User user = authService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", user.id().toString(), "email", user.email()));
    }

    /**
     * Authenticates a user and issues a new access and refresh token
     *
     * <p>Credentials are checked by {@link AuthService#login(String, String)}.
     * If valid, an access token is created by {@link TokenIssuer#issue(User)}
     * and a refresh token is created by {@link RefreshTokenService#issue}</p>
     *
     * @param request the validated email and password submitted by the client
     * @return {@code 200 OK} with JSON body containing {@code id}. {@code email},
     *         {@code accessToken}, {@code refreshToken}, {@code tokenType} (always {@code "Bearer"})
     *         and {@code expiresIn} (access token lifetime in seconds)
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request){
        User user = authService.login(request.email(), request.password());
        AccessToken token = tokenIssuer.issue(user);
        String refreshToken = refreshTokenService.issue(user.id());

        return ResponseEntity.ok(Map.of(
                "id", user.id().toString(),
                "email", user.email(),
                "accessToken", token.value(),
                "refreshToken", refreshToken,
                "tokenType", "Bearer",
                "expiresIn", token.expiresInSeconds()));
    }

    /**
     * Returns the identity of the currently authenticated caller.
     *
     * <p>This endpoint requires a valid access token.
     * Spring Security Validates the {@code Authorization: Bearer}
     * header and stores the result in the security context as a
     * {@code JwtAuthenticationToken}</p>
     *
     * {@code @AuthenticationPrincipal} takes the {@code principal} field of that
     * {@code JwtAuthenticationToken} (in this case, the decoded {@link Jwt})
     * and injects it as a method parameter
     *
     * @param jwt the validated access token of the caller, injected by Spring Security
     * @return {@code 200 OK} with a JSON body containing the {@code id} (token subject)
     *         and {@code email} (token claim) of the caller
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
                "id", jwt.getSubject(),
                "email", jwt.getClaimAsString("email")));
    }

    /**
     * Exchanges a refresh token for a new access token and a new refresh token.
     *
     * <p>The submitted refresh token is rotated by {@link RefreshTokenService#rotate}, meaning the
     * old token is replaced by a new one. The client must store the new refresh token and
     * discard the old one.
     *
     * @param request the validated request containing the refresh token to rotate
     * @return {@code 200 OK} with a JSON body containing {@code id}, {@code email},
     *         {@code accessToken}, the new {@code refreshToken}, {@code tokenType}
     *         (always {@code "Bearer"}) and {@code expiresIn} (access token lifetime in seconds)
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest request){
        RotatedToken rotated = refreshTokenService.rotate(request.refreshToken());
        User user = authService.getUser(rotated.userId());
        AccessToken accessToken = tokenIssuer.issue(user);

        return ResponseEntity.ok(Map.of(
                "id", user.id().toString(),
                "email", user.email(),
                "accessToken", accessToken.value(),
                "refreshToken", rotated.refreshToken(),
                "tokenType", "Bearer",
                "expiresIn", accessToken.expiresInSeconds()
        ));
    }

    /**
     * Logs a user out by revoking the supplied refresh token, so it can no longer be
     * used to obtain new access tokens.
     *
     * <p>Access tokens already issued remain valid until they expire (15 minutes maximum).
     *
     * @param request the validated request containing the refresh token to revoke
     * @return {@code 204 No Content} with an empty body
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request){
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
