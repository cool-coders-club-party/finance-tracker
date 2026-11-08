package ie.universityofgalway.finance.transactions.it;

import ie.universityofgalway.finance.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static ie.universityofgalway.finance.transactions.util.DataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionRestoreTests {
    private static final String API = "/api/v1/transactions";
    @Autowired MockMvc mvc;
    @Autowired TransactionRepository repository;

    @Test
    @DisplayName("Restore commits to the database and returns the expense to history and summaries")
    void givenSoftDeletedExpense_whenRestored_thenPersistedAndIncludedAgain() throws Exception {
        //given
        UUID owner = UUID.randomUUID();
        var expense = groceries();
        expense.setUserId(owner);
        expense.setDescription("Weekly groceries");
        Long id = repository.saveAndFlush(expense).getId();
        var saved = repository.findById(id).orElseThrow();
        try {
            mvc.perform(delete(API + "/{id}", id).with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk());
            assertThat(repository.findById(id).orElseThrow().getActive()).isFalse();
            mvc.perform(get(API).with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
            mvc.perform(get(API + "/summary").param("currency", "USD")
                            .with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.expenses").value(0));

            mvc.perform(get(API + "/deleted").with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(id));
            mvc.perform(get(API + "/deleted").param("userId", owner.toString())
                            .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));

            //when
            var result = mvc.perform(post(API + "/{id}/restore", id)
                    .with(jwt().jwt(token -> token.subject(owner.toString()))));

            //then
            result.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
            mvc.perform(get(API + "/deleted").with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
            var restored = repository.findById(id).orElseThrow();
            assertThat(restored.getActive()).isTrue();
            assertThat(restored).usingRecursiveComparison().ignoringFields("active").isEqualTo(saved);
            mvc.perform(get(API + "/{id}", id).with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.amount").value(10.5));
            mvc.perform(get(API).with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(id));
            mvc.perform(get(API + "/summary").param("currency", "USD")
                            .with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.expenses").value(10.5))
                    .andExpect(jsonPath("$.netIncome").value(-10.5))
                    .andExpect(jsonPath("$.spendingByCategory[0].category").value("GROCERIES"))
                    .andExpect(jsonPath("$.spendingByCategory[0].amount").value(10.5));
            mvc.perform(post(API + "/{id}/restore", id).with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isNotFound());
            assertThat(repository.findById(id).orElseThrow().getActive()).isTrue();
        } finally {
            repository.deleteById(id);
        }
    }

    @Test
    @DisplayName("Another user cannot restore a deleted transaction even when supplying its owner's ID")
    void givenAnotherUsersDeletedTransaction_whenRestored_thenNotFoundAndStillInactive() throws Exception {
        //given
        var deleted = groceries();
        deleted.setActive(false);
        Long id = repository.saveAndFlush(deleted).getId();
        try {
            //when
            var result = mvc.perform(post(API + "/{id}/restore", id).param("userId", USER_ID.toString())
                    .with(jwt().jwt(token -> token.subject(OTHER_USER_ID.toString()))));

            //then
            result.andExpect(status().isNotFound());
            var retained = repository.findById(id).orElseThrow();
            assertThat(retained.getActive()).isFalse();
            assertThat(retained.getUserId()).isEqualTo(USER_ID);
        } finally {
            repository.deleteById(id);
        }
    }

    @Test
    @DisplayName("Permanently deleted transactions cannot be restored")
    void givenPermanentlyDeletedTransaction_whenRestored_thenNotFoundAndNotRecreated() throws Exception {
        //given
        Long id = repository.saveAndFlush(groceries()).getId();
        repository.deleteById(id);

        //when
        var result = mvc.perform(post(API + "/{id}/restore", id)
                .with(jwt().jwt(token -> token.subject(USER_ID.toString()))));

        //then
        result.andExpect(status().isNotFound());
        assertThat(repository.findById(id)).isEmpty();
    }
}
