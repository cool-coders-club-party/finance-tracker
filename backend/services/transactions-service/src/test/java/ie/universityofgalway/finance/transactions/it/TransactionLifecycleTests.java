package ie.universityofgalway.finance.transactions.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static ie.universityofgalway.finance.transactions.util.DataUtils.USER_ID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionLifecycleTests {
    private static final String API = "/api/v1/transactions";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    @DisplayName("A saved expense contributes to the user's summary until it is soft-deleted")
    void givenRecordedExpense_whenSoftDeleted_thenSummaryAndHistoryExcludeIt() throws Exception {
        //given
        String created = mvc.perform(post(API).with(jwt().jwt(token -> token.subject(USER_ID.toString()))).contentType(APPLICATION_JSON).content("""
                {"transactionType":"EXPENSE","transactionCategory":"GROCERIES",
                 "currency":"GBP","amount":"42.50","occurredAt":"2025-03-10T12:00:00"}
                """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        mvc.perform(get(API + "/summary").with(jwt().jwt(token -> token.subject(USER_ID.toString()))).param("currency", "GBP"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expenses").value(42.5))
                .andExpect(jsonPath("$.spendingByCategory[0].category").value("GROCERIES"));
        //when
        var deleted = mvc.perform(delete(API + "/{id}", id).with(jwt().jwt(token -> token.subject(USER_ID.toString()))));
        //then
        deleted.andExpect(status().isOk());
        mvc.perform(get(API + "/summary").with(jwt().jwt(token -> token.subject(USER_ID.toString()))).param("currency", "GBP"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expenses").value(0.0))
                .andExpect(jsonPath("$.spendingByCategory").isEmpty());
        mvc.perform(get(API).with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
}
