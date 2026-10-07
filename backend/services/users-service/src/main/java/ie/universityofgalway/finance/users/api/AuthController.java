package ie.universityofgalway.finance.users.api;

import ie.universityofgalway.finance.users.api.dto.RegisterRequest; // pull in DTO
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
     * <p>Note: this is a placeholder. It currently only echoes the submitted email and does
     * not yet check for duplicates, hash the password or store the user.
     *
     * @param request the validated email and password submitted by the client
     * @return {@code 201 Created} with a JSON body containing the registered email
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("email", request.email()));
    }
}
