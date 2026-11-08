package ie.universityofgalway.finance.transactions.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    @DisplayName("Only the matching owner can retrieve an active transaction")
    void givenActiveTransaction_whenAnotherOwnerLooksUp_thenRowIsHidden() {
        //given
        Long id = repository.saveAndFlush(groceries()).getId();
        entityManager.clear();

        //when
        var ownerResult = repository.findActiveByIdAndUserId(id, USER_ID);
        var otherResult = repository.findActiveByIdAndUserId(id, OTHER_USER_ID);

        //then
        assertThat(ownerResult).isPresent();
        assertThat(otherResult).isEmpty();
    }

    @Test
    @DisplayName("Soft deletion hides the transaction but retains its amount and currency")
    void givenActiveTransaction_whenSoftDeleted_thenRowIsRetainedButHidden() {
        //given
        Long id = repository.saveAndFlush(groceries()).getId();

        //when
        int affected = repository.softDelete(id, USER_ID);
        entityManager.clear();

        //then
        assertThat(affected).isOne();
        assertThat(repository.findActiveByIdAndUserId(id, USER_ID)).isEmpty();
        Transaction retained = repository.findById(id).orElseThrow();
        assertThat(retained.getActive()).isFalse();
        assertThat(retained.getAmount()).isEqualByComparingTo("10.50");
        assertThat(retained.getCurrency().name()).isEqualTo("USD");
        assertThat(repository.softDelete(id, USER_ID)).isZero();
    }

    @Test
    @DisplayName("Permanent deletion removes a previously soft-deleted row")
    void givenInactiveTransaction_whenHardDeleted_thenRowIsRemoved() {
        //given
        Long id = repository.saveAndFlush(groceries()).getId();
        repository.softDelete(id, USER_ID);

        //when
        int affected = repository.hardDelete(id, USER_ID);
        entityManager.clear();

        //then
        assertThat(affected).isOne();
        assertThat(repository.findById(id)).isEmpty();
        assertThat(repository.hardDelete(id, USER_ID)).isZero();
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
        otherUser.setUserId(OTHER_USER_ID);
        Transaction inactive = groceries();
        inactive.setActive(false);
        Transaction gift = groceries();
        gift.setTransactionCategory(GIFT);
        Transaction income = groceries();
        income.setTransactionType(INCOME);
        income.setTransactionCategory(GIFT);
        repository.saveAllAndFlush(List.of(first, last, tomorrow, yesterday, otherUser, inactive, gift, income));

        //when
        var matches = repository.findAll(TransactionSpecifications.forHistory(USER_ID, EXPENSE, GROCERIES, DAY, DAY));
        var expenses = repository.findAll(TransactionSpecifications.forHistory(USER_ID, EXPENSE, null, DAY, DAY));

        //then
        assertThat(matches).extracting(Transaction::getId).containsExactlyInAnyOrder(first.getId(), last.getId());
        assertThat(expenses).extracting(Transaction::getId).containsExactlyInAnyOrder(first.getId(), last.getId(), gift.getId());
    }

    @Test
    @DisplayName("Restore lookup returns only an inactive transaction belonging to the owner")
    void givenInactiveTransaction_whenLookedUpForRestore_thenOnlyOwnerCanFindIt() {
        //given
        Transaction deleted = groceries();
        deleted.setActive(false);
        Long id = repository.saveAndFlush(deleted).getId();
        entityManager.clear();

        //when
        var ownerResult = repository.findByIdAndUserIdAndActiveFalse(id, USER_ID);
        var otherResult = repository.findByIdAndUserIdAndActiveFalse(id, OTHER_USER_ID);

        //then
        assertThat(ownerResult).isPresent();
        assertThat(ownerResult.orElseThrow().getActive()).isFalse();
        assertThat(otherResult).isEmpty();
    }

    @Test
    @DisplayName("Restore lookup excludes active and permanently deleted transactions")
    void givenActiveAndRemovedTransactions_whenLookedUpForRestore_thenNeitherIsReturned() {
        //given
        Long activeId = repository.saveAndFlush(groceries()).getId();
        Long removedId = repository.saveAndFlush(groceries()).getId();
        repository.hardDelete(removedId, USER_ID);
        entityManager.clear();

        //when
        var activeResult = repository.findByIdAndUserIdAndActiveFalse(activeId, USER_ID);
        var removedResult = repository.findByIdAndUserIdAndActiveFalse(removedId, USER_ID);

        //then
        assertThat(activeResult).isEmpty();
        assertThat(removedResult).isEmpty();
    }


    @Test
    @DisplayName("Deleted history isolates the owner, excludes active rows and applies each optional filter")
    void givenMixedTransactions_whenDeletedHistoryFiltered_thenOnlyEligibleRowsAreReturned() {
        //given
        var owner = java.util.UUID.randomUUID();
        Transaction first = groceries();
        first.setOccurredAt(DAY.atStartOfDay());
        Transaction last = groceries();
        last.setOccurredAt(DAY.atTime(23, 59, 59, 999000000));
        Transaction yesterday = groceries();
        yesterday.setOccurredAt(DAY.minusDays(1).atTime(23, 59, 59));
        Transaction tomorrow = groceries();
        tomorrow.setOccurredAt(DAY.plusDays(1).atStartOfDay());
        Transaction otherOwner = groceries();
        otherOwner.setUserId(OTHER_USER_ID);
        Transaction gift = groceries();
        gift.setTransactionCategory(GIFT);
        Transaction income = groceries();
        income.setTransactionCategory(GIFT);
        income.setTransactionType(INCOME);
        var deleted = List.of(first, last, yesterday, tomorrow, otherOwner, gift, income);
        deleted.forEach(transaction -> {
            transaction.setActive(false);
            if (transaction != otherOwner) transaction.setUserId(owner);
        });
        repository.saveAllAndFlush(deleted);
        Transaction active = groceries();
        active.setUserId(owner);
        repository.saveAndFlush(active);
        entityManager.clear();

        //when
        var all = repository.findAll(TransactionSpecifications.forSoftDeleted(owner, null, null, null, null));
        var combined = repository.findAll(TransactionSpecifications.forSoftDeleted(owner, EXPENSE, GROCERIES, DAY, DAY));
        var byType = repository.findAll(TransactionSpecifications.forSoftDeleted(owner, INCOME, null, null, null));
        var byCategory = repository.findAll(TransactionSpecifications.forSoftDeleted(owner, null, GIFT, null, null));
        var fromOnly = repository.findAll(TransactionSpecifications.forSoftDeleted(owner, null, null, DAY, null));
        var toOnly = repository.findAll(TransactionSpecifications.forSoftDeleted(owner, null, null, null, DAY));

        //then
        assertThat(all).extracting(Transaction::getId).containsExactlyInAnyOrder(
                first.getId(), last.getId(), yesterday.getId(), tomorrow.getId(), gift.getId(), income.getId());
        assertThat(combined).extracting(Transaction::getId).containsExactlyInAnyOrder(first.getId(), last.getId());
        assertThat(byType).extracting(Transaction::getId).containsExactly(income.getId());
        assertThat(byCategory).extracting(Transaction::getId).containsExactlyInAnyOrder(gift.getId(), income.getId());
        assertThat(fromOnly).extracting(Transaction::getId).containsExactlyInAnyOrder(
                first.getId(), last.getId(), tomorrow.getId(), gift.getId(), income.getId());
        assertThat(toOnly).extracting(Transaction::getId).containsExactlyInAnyOrder(
                first.getId(), last.getId(), yesterday.getId(), gift.getId(), income.getId());
    }

    @Test
    @DisplayName("Deleted history pages have stable ordering, accurate totals and no overlap")
    void givenEqualDatedDeletedTransactions_whenPaged_thenStablePagesAndTotalsAreReturned() {
        //given
        var owner = java.util.UUID.randomUUID();
        var records = List.of(groceries(), groceries(), groceries());
        records.forEach(transaction -> {
            transaction.setActive(false);
            transaction.setUserId(owner);
        });
        repository.saveAllAndFlush(records);
        Transaction active = groceries();
        active.setUserId(owner);
        repository.saveAndFlush(active);
        entityManager.clear();
        var specification = TransactionSpecifications.forSoftDeleted(owner, null, null, null, null);
        var sort = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));

        //when
        var first = repository.findAll(specification, PageRequest.of(0, 2, sort));
        var second = repository.findAll(specification, PageRequest.of(1, 2, sort));
        var beyondLast = repository.findAll(specification, PageRequest.of(2, 2, sort));

        //then
        assertThat(first.getContent()).extracting(Transaction::getId)
                .containsExactly(records.get(2).getId(), records.get(1).getId());
        assertThat(second.getContent()).extracting(Transaction::getId).containsExactly(records.get(0).getId());
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(second.getTotalElements()).isEqualTo(3);
        assertThat(beyondLast.getContent()).isEmpty();
        assertThat(beyondLast.getTotalElements()).isEqualTo(3);
    }

}
