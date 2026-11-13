package ie.universityofgalway.finance.users.api;

import ie.universityofgalway.finance.users.api.dto.LoginRequest;
import ie.universityofgalway.finance.users.api.dto.RefreshTokenRequest;
import ie.universityofgalway.finance.users.api.dto.RegisterRequest; // pull in DTO
import ie.universityofgalway.finance.users.application.*;
import ie.universityofgalway.finance.users.domain.User;
import jakarta.validation.Valid;
import org.antlr.v4.runtime.Token;
import org.apache.coyote.Response;
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
 *   <li>{@code POST /api/auth/register} - create a new account</li>
 * </ul>
 *
 * <p>This class only translates between HTTP and Java: it receives requests, triggers
 * validation and builds responses. Business rules belong in the application layer.
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
     * <p> {@code @Valid}
     *     {@code @RequestBody}
     *
     * <p>Note: this is a placeholder. It currently only echoes the submitted email and does
     * not yet check for duplicates, hash the password or store the user.
     *
     * @param request the validated email and password submitted by the client
     * @return {@code 201 Created} with a JSON body containing the registered email
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@Valid @RequestBody RegisterRequest request){
        User user = authService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", user.id().toString(), "email", user.email()));
    }

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

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
                "id", jwt.getSubject(),
                "email", jwt.getClaimAsString("email")));
    }

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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request){
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
