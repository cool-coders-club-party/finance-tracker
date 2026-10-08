package ie.universityofgalway.finance.users.api;

import ie.universityofgalway.finance.users.application.AuthService;
import ie.universityofgalway.finance.users.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc is a tool that sends fake HTTP requests and sends it to Spring MVC for testing.<p>
 * {@code @WebMvcTest} builds it, {@code @Autowired} pairs it to a field.<p>
 * {@code @WebMvcTest} runs only the controller (i.e. don't run any services or repositories)<p>
 * But AuthController needs an AuthService in its constructor.<p>
 * {@code @MockitoBean} creates a mock service to be used by Spring. A bean is a Spring method.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mvc; // spoof HTTP requests

    private static final String VALID_JSON_BODY =
            """
            {"email": "test@example.com", "password": "12345678"}
            """; // min 8 char password constraint set in RegisterRequest

    @MockitoBean
    private AuthService authService; // mock AuthService

    /**
     * Passing behavior to Mockito for authService to ensure tests run correctly
     * An un-stubbed mock service returns null, which would crash the controller.
     */
    @BeforeEach
    void stubService(){
        when(authService.register(anyString(), anyString())).thenAnswer(inv -> new User(UUID.randomUUID(), inv.getArgument(0)));
    }

    @Test
    void pingReturns200WithServiceMessage() throws Exception {
        mvc.perform(get("/api/auth/ping")) // send GET request
                .andExpect(status().isOk()) // expect code 200 OK in response's status line
                .andExpect(content().string("auth api is working")); // expect relevant message in response body
    }

    /* setup for POST tests. Will be chained with .andExpect() in tests */
    private ResultActions postRegister(String json) throws Exception{
        return mvc.perform(post("/api/auth/register") // send POST request
                .contentType(MediaType.APPLICATION_JSON) // set Content-Type header in response
                .content(json));
    }

    /* like printf(), uses conversion characters to inject Strings into JSON body */
    private static String json(String email, String password){
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    @Test void registerWithAValidBodyReturns201() throws Exception{
        postRegister(VALID_JSON_BODY).andExpect(status().isCreated()); // check for status 201
    }

    @Test
    void registerResponseContainsIDAndEmail() throws Exception{
        postRegister(VALID_JSON_BODY)
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @ParameterizedTest // run the same test once per row
    @CsvSource({
            "invalid_email, 12345678",
            "'', 12345678",
            "test@example.com, 12345",
            "test@example.com, 1234567",
            "test@example.com, ''"
    })// each row has its values passed in one-by-one as arguments (i.e. each row has two arguments, for a total of 5 tests)
    // Each row is a case, and the columns fill the parameters in order
    void registerWithInvalidFieldsReturns400(String email, String password) throws Exception{
        postRegister(json(email, password))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithEmptyObjectReturns400() throws Exception{
        postRegister("{}")
                .andExpect(status().isBadRequest());
    }

    /**
     * Testing boundary of BCrypt's input limit (72 chars)
     */
    @Test
    void registerWithPasswordOf72CharactersReturns201() throws Exception{
        postRegister(json("text@example.com", "a".repeat(72)))
                .andExpect(status().isCreated());
    }

    @Test
    void registerWithPasswordOf73CharactersReturns400() throws Exception{
        postRegister(json("text@example.com", "a".repeat(73)))
                .andExpect(status().isBadRequest());
    }
}