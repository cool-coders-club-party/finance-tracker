package ie.universityofgalway.finance.transactions.repository;

import ie.universityofgalway.finance.transactions.entity.Transaction;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static ie.universityofgalway.finance.transactions.entity.TransactionCategory.GROCERIES;
import static ie.universityofgalway.finance.transactions.entity.TransactionCategory.GIFT;
import static ie.universityofgalway.finance.transactions.entity.TransactionType.EXPENSE;
import static ie.universityofgalway.finance.transactions.entity.TransactionType.INCOME;
import static ie.universityofgalway.finance.transactions.util.DataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTests {
    @Autowired TransactionRepository repository;
    @Autowired EntityManager entityManager;

    @Test
    @DisplayName("Soft deletion hides the transaction but retains its amount and currency")
    void givenActiveTransaction_whenSoftDeleted_thenRowIsRetainedButHidden() {
        //given
        Long id = repository.saveAndFlush(groceries()).getId();

        //when
        int affected = repository.softDelete(id);
        entityManager.clear();

        //then
        assertThat(affected).isOne();
        assertThat(repository.findActiveById(id)).isEmpty();
        Transaction retained = repository.findById(id).orElseThrow();
        assertThat(retained.getActive()).isFalse();
        assertThat(retained.getAmount()).isEqualByComparingTo("10.50");
        assertThat(retained.getCurrency().name()).isEqualTo("USD");
        assertThat(repository.softDelete(id)).isZero();
    }

    @Test
    @DisplayName("Permanent deletion removes a previously soft-deleted row")
    void givenInactiveTransaction_whenHardDeleted_thenRowIsRemoved() {
        //given
        Long id = repository.saveAndFlush(groceries()).getId();
        repository.softDelete(id);

        //when
        int affected = repository.hardDelete(id);
        entityManager.clear();

        //then
        assertThat(affected).isOne();
        assertThat(repository.findById(id)).isEmpty();
        assertThat(repository.hardDelete(id)).isZero();
    }

    @Test
    @DisplayName("History includes both date boundaries and isolates the user's active expenses")
    void givenMixedTransactions_whenHistoryIsFiltered_thenOnlyMatchingRowsAreReturned() {
        //given
        Transaction first = groceries();
        first.setOccurredAt(DAY.atStartOfDay());
        Transaction last = groceries();
        last.setOccurredAt(DAY.atTime(23, 59, 59));
        Transaction tomorrow = groceries();
        tomorrow.setOccurredAt(DAY.plusDays(1).atStartOfDay());
        Transaction yesterday = groceries();
        yesterday.setOccurredAt(DAY.minusDays(1).atTime(23, 59));
        Transaction otherUser = groceries();
        otherUser.setUserId(43L);
        Transaction inactive = groceries();
        inactive.setActive(false);
        Transaction gift = groceries();
        gift.setTransactionCategory(GIFT);
        Transaction income = groceries();
        income.setTransactionType(INCOME);
        income.setTransactionCategory(GIFT);
        repository.saveAllAndFlush(List.of(first, last, tomorrow, yesterday, otherUser, inactive, gift, income));

        //when
        var matches = repository.findAll(TransactionSpecifications.forHistory(42L, EXPENSE, GROCERIES, DAY, DAY));
        var expenses = repository.findAll(TransactionSpecifications.forHistory(42L, EXPENSE, null, DAY, DAY));

        //then
        assertThat(matches).extracting(Transaction::getId).containsExactlyInAnyOrder(first.getId(), last.getId());
        assertThat(expenses).extracting(Transaction::getId).containsExactlyInAnyOrder(first.getId(), last.getId(), gift.getId());
    }
}
