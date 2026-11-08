package ie.universityofgalway.finance.transactions.it;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import ie.universityofgalway.finance.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static ie.universityofgalway.finance.transactions.util.DataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticatedCreationTests {
    private static final String API = "/api/v1/transactions";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TransactionRepository repository;
    @Value("${app.jwt.secret}") String secret;

    @Test
    @DisplayName("Creation persists the verified token owner even when the body supplies another user")
    void givenSignedTokenAndForgedOwner_whenCreated_thenTokenOwnerIsPersisted() throws Exception {
        //given
        String body = """
                {"userId":"%s","transactionType":"EXPENSE","transactionCategory":"GROCERIES",
                 "currency":"EUR","amount":"42.50","occurredAt":"2025-03-10T12:00:00"}
                """.formatted(OTHER_USER_ID);
        String token = token(secret, Instant.now().plusSeconds(300));
        //when
        var result = mvc.perform(post(API).header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content(body));
        //then
        String response = result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).get("id").asLong();
        try {
            var stored = repository.findById(id).orElseThrow();
            assertThat(stored.getUserId()).isEqualTo(USER_ID);
            assertThat(stored.getAmount()).isEqualByComparingTo("42.50");
        } finally {
            repository.deleteById(id);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "expired", "wrong-signature"})
    @DisplayName("Missing, expired and incorrectly signed tokens cannot create a transaction")
    void givenInvalidAuthentication_whenCreated_thenReturnsUnauthorized(String scenario) throws Exception {
        //given
        long count = repository.count();
        var request = post(API).contentType(APPLICATION_JSON).content("""
                {"transactionType":"EXPENSE","transactionCategory":"GROCERIES",
                 "currency":"EUR","amount":"42.50","occurredAt":"2025-03-10T12:00:00"}
                """);
        if (scenario.equals("expired")) {
            request.header("Authorization", "Bearer " + token(secret, Instant.now().minusSeconds(120)));
        } else if (scenario.equals("wrong-signature")) {
            request.header("Authorization", "Bearer " + token(
                    "another-test-only-secret-with-32-bytes", Instant.now().plusSeconds(300)));
        }
        //when
        var result = mvc.perform(request);
        //then
        result.andExpect(status().isUnauthorized());
        assertThat(repository.count()).isEqualTo(count);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Another user cannot read, update or delete an owner's transaction")
    void givenAnotherUser_whenAccessingTransaction_thenReturnsNotFound(boolean permanent) throws Exception {
        //given
        long id = repository.saveAndFlush(groceries()).getId();
        var otherUser = jwt().jwt(token -> token.subject(OTHER_USER_ID.toString()));
        try {
            //when
            var read = mvc.perform(get(API + "/{id}", id).with(otherUser));
            var update = mvc.perform(patch(API + "/{id}", id).with(otherUser)
                    .contentType(APPLICATION_JSON).content("{\"amount\":\"99.00\"}"));
            var deletion = mvc.perform(delete(API + "/{id}", id).with(otherUser)
                    .param("permanent", Boolean.toString(permanent)));

            //then
            read.andExpect(status().isNotFound());
            update.andExpect(status().isNotFound());
            deletion.andExpect(status().isNotFound());
            var stored = repository.findById(id).orElseThrow();
            assertThat(stored.getActive()).isTrue();
            assertThat(stored.getAmount()).isEqualByComparingTo("10.50");
            assertThat(stored.getUserId()).isEqualTo(USER_ID);
            mvc.perform(delete(API + "/{id}", id)
                    .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                    .param("permanent", Boolean.toString(permanent)))
                    .andExpect(status().isOk());
            if (permanent) {
                assertThat(repository.findById(id)).isEmpty();
            } else {
                assertThat(repository.findById(id).orElseThrow().getActive()).isFalse();
            }
        } finally {
            repository.findById(id).ifPresent(repository::delete);
        }
    }

    private String token(String signingSecret, Instant expiry) {
        var key = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var claims = JwtClaimsSet.builder().subject(USER_ID.toString())
                .issuedAt(Instant.now().minusSeconds(600)).expiresAt(expiry).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
