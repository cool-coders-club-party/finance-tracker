package ie.universityofgalway.finance.transactions.it;

import ie.universityofgalway.finance.transactions.entity.Transaction;
import ie.universityofgalway.finance.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static ie.universityofgalway.finance.transactions.entity.TransactionCategory.*;
import static ie.universityofgalway.finance.transactions.entity.TransactionType.INCOME;
import static ie.universityofgalway.finance.transactions.util.DataUtils.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionIncomeSummaryTests {
    private static final String API = "/api/v1/transactions";
    @Autowired MockMvc mvc;
    @Autowired TransactionRepository repository;

    @Test
    @DisplayName("Income breakdown includes only the owner's active income within inclusive date boundaries")
    void givenMixedOwnersStatusesAndDates_whenSummaryRequested_thenOnlyMatchingIncomeContributes() throws Exception {
        //given
        UUID owner = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();
        var first = income(owner, "10.00");
        first.setOccurredAt(DAY.atStartOfDay());
        var last = income(owner, "20.00");
        last.setOccurredAt(DAY.atTime(23, 59, 59, 999_000_000));
        var freelance = income(owner, "5.00");
        freelance.setTransactionCategory(FREELANCE);
        var before = income(owner, "100.00");
        before.setOccurredAt(DAY.minusDays(1).atTime(23, 59, 59));
        var after = income(owner, "200.00");
        after.setOccurredAt(DAY.plusDays(1).atStartOfDay());
        var deleted = income(owner, "300.00");
        deleted.setActive(false);
        var foreign = income(otherOwner, "400.00");
        var expense = groceries();
        expense.setUserId(owner);
        expense.setAmount(new BigDecimal("2.00"));
        List<Transaction> records = repository.saveAllAndFlush(
                List.of(first, last, freelance, before, after, deleted, foreign, expense));
        try {
            //when
            var result = mvc.perform(get(API + "/summary")
                    .with(jwt().jwt(token -> token.subject(owner.toString())))
                    .param("userId", otherOwner.toString())
                    .param("currency", "USD")
                    .param("from", DAY.toString()).param("to", DAY.toString()));

            //then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.income").value(35.0))
                    .andExpect(jsonPath("$.expenses").value(2.0))
                    .andExpect(jsonPath("$.netIncome").value(33.0))
                    .andExpect(jsonPath("$.incomeByCategory.length()").value(2))
                    .andExpect(jsonPath("$.incomeByCategory[?(@.category == 'SALARY')].amount",
                            org.hamcrest.Matchers.contains(30.0)))
                    .andExpect(jsonPath("$.incomeByCategory[?(@.category == 'FREELANCE')].amount",
                            org.hamcrest.Matchers.contains(5.0)))
                    .andExpect(jsonPath("$.spendingByCategory.length()").value(1))
                    .andExpect(jsonPath("$.spendingByCategory[0].category").value("GROCERIES"));
        } finally {
            repository.deleteAllById(records.stream().map(Transaction::getId).toList());
        }
    }

    @Test
    @DisplayName("Deleting and restoring income updates its category and summary totals")
    void givenIncome_whenDeletedAndRestored_thenBreakdownTracksPersistedLifecycle() throws Exception {
        //given
        UUID owner = UUID.randomUUID();
        Long id = repository.saveAndFlush(income(owner, "42.50")).getId();
        try {
            mvc.perform(get(API + "/summary").param("currency", "USD")
                            .with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.income").value(42.5))
                    .andExpect(jsonPath("$.incomeByCategory[0].category").value("SALARY"));

            //when
            mvc.perform(delete(API + "/{id}", id)
                            .with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk());

            //then
            mvc.perform(get(API + "/summary").param("currency", "USD")
                            .with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.income").value(0.0))
                    .andExpect(jsonPath("$.netIncome").value(0.0))
                    .andExpect(jsonPath("$.incomeByCategory").isEmpty());

            //when
            mvc.perform(post(API + "/{id}/restore", id)
                            .with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk());

            //then
            mvc.perform(get(API + "/summary").param("currency", "USD")
                            .with(jwt().jwt(token -> token.subject(owner.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.income").value(42.5))
                    .andExpect(jsonPath("$.expenses").value(0.0))
                    .andExpect(jsonPath("$.netIncome").value(42.5))
                    .andExpect(jsonPath("$.incomeByCategory.length()").value(1))
                    .andExpect(jsonPath("$.incomeByCategory[0].category").value("SALARY"))
                    .andExpect(jsonPath("$.incomeByCategory[0].amount").value(42.5))
                    .andExpect(jsonPath("$.spendingByCategory").isEmpty());
        } finally {
            repository.deleteById(id);
        }
    }

    private static Transaction income(UUID owner, String amount) {
        var transaction = groceries();
        transaction.setUserId(owner);
        transaction.setTransactionType(INCOME);
        transaction.setTransactionCategory(SALARY);
        transaction.setAmount(new BigDecimal(amount));
        return transaction;
    }
}
