package ie.universityofgalway.finance.transactions.service;

import ie.universityofgalway.finance.transactions.exception.InvalidTransactionHistoryQueryException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ie.universityofgalway.finance.transactions.entity.Transaction;
import ie.universityofgalway.finance.transactions.exception.InvalidTransactionCombinationException;
import ie.universityofgalway.finance.transactions.exception.TransactionNotFoundException;
import ie.universityofgalway.finance.transactions.mapper.TransactionMapper;
import ie.universityofgalway.finance.transactions.repository.TransactionRepository;
import ie.universityofgalway.finance.transactions.rest.dto.CreateTransactionRequest;
import ie.universityofgalway.finance.transactions.rest.dto.UpdateTransactionRequest;
import ie.universityofgalway.finance.transactions.util.validator.TransactionCategoryValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static ie.universityofgalway.finance.transactions.entity.TransactionCategory.*;
import static ie.universityofgalway.finance.transactions.entity.TransactionCurrency.*;
import static ie.universityofgalway.finance.transactions.entity.TransactionType.*;
import static ie.universityofgalway.finance.transactions.util.DataUtils.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTests {
    @Mock TransactionRepository repository;
    TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(repository, Mappers.getMapper(TransactionMapper.class),
                new TransactionCategoryValidator());
    }

    @Test
    @DisplayName("Creation returns the saved ID, currency and original amount")
    void givenValidExpense_whenCreated_thenSavedTransactionIsReturned() {
        //given
        var request = new CreateTransactionRequest( EXPENSE, GROCERIES, GBP,
                new BigDecimal("12.34"), "Lunch", DAY.atTime(12, 0));
        given(repository.save(any(Transaction.class))).willAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(17L);
            return saved;
        });
        //when
        var response = service.createTransaction(request, USER_ID);
        //then
        assertThat(response.id()).isEqualTo(17L);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.currency()).isEqualTo(GBP);
        assertThat(response.amount()).isEqualByComparingTo("12.34");
    }

    @Test
    @DisplayName("Changing only the denomination preserves the original amount")
    void givenUsdExpense_whenCurrencyPatched_thenOnlyCurrencyChanges() {
        //given
        given(repository.findActiveByIdAndUserId(7L, USER_ID)).willReturn(Optional.of(groceries()));
        var patch = new UpdateTransactionRequest(null, null, null, EUR, null, null);
        //when
        var response = service.updateTransaction(7L, patch, USER_ID);
        //then
        assertThat(response.currency()).isEqualTo(EUR);
        assertThat(response.amount()).isEqualByComparingTo("10.50");
        assertThat(response.transactionCategory()).isEqualTo(GROCERIES);
    }

    @Test
    @DisplayName("An amount-only patch preserves the saved currency")
    void givenUsdExpense_whenAmountPatched_thenCurrencyIsPreserved() {
        //given
        given(repository.findActiveByIdAndUserId(7L, USER_ID)).willReturn(Optional.of(groceries()));
        var patch = new UpdateTransactionRequest(new BigDecimal("20.00"), null, null, null, null, null);
        //when
        var response = service.updateTransaction(7L, patch, USER_ID);
        //then
        assertThat(response.amount()).isEqualByComparingTo("20");
        assertThat(response.currency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("A type-only patch must be compatible with the existing category")
    void givenGroceriesExpense_whenChangedToIncome_thenPatchIsRejected() {
        //given
        var saved = groceries();
        given(repository.findActiveByIdAndUserId(7L, USER_ID)).willReturn(Optional.of(saved));
        var patch = new UpdateTransactionRequest(null, null, null, null, INCOME, null);
        //when
        Throwable error = catchThrowable(() -> service.updateTransaction(7L, patch, USER_ID));
        //then
        assertThat(error).isInstanceOf(InvalidTransactionCombinationException.class);
        assertThat(saved.getTransactionType()).isEqualTo(EXPENSE);
    }

    @Test
    @DisplayName("A missing active transaction is reported as not found")
    void givenNoActiveTransaction_whenRead_thenNotFound() {
        //given
        given(repository.findActiveByIdAndUserId(7L, USER_ID)).willReturn(Optional.empty());
        //when
        Throwable error = catchThrowable(() -> service.findTransactionById(7L, USER_ID));
        //then
        assertThat(error).isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    @DisplayName("Metadata describes income-only, expense-only and shared categories")
    void givenSupportedCategories_whenMetadataRequested_thenAllowedTypesArePublished() {
        //given
        var expectedCurrencies = java.util.List.of(EUR, USD, GBP);
        //when
        var metadata = service.findTransactionMetadata();
        //then
        assertThat(metadata.currencies()).containsExactlyInAnyOrderElementsOf(expectedCurrencies);
        assertThat(metadata.categories()).anySatisfy(c -> {
            assertThat(c.name()).isEqualTo(GIFT);
            assertThat(c.allowedTypes()).containsExactlyInAnyOrder(INCOME, EXPENSE);
        }).anySatisfy(c -> {
            assertThat(c.name()).isEqualTo(SALARY);
            assertThat(c.allowedTypes()).containsExactly(INCOME);
        }).anySatisfy(c -> {
            assertThat(c.name()).isEqualTo(GROCERIES);
            assertThat(c.allowedTypes()).containsExactly(EXPENSE);
        });
    }

    @Test
    @DisplayName("Restore reactivates the transaction and preserves its original details")
    void givenInactiveTransaction_whenRestored_thenOriginalTransactionIsReactivated() {
        //given
        Transaction deleted = groceries();
        deleted.setId(7L);
        deleted.setActive(false);
        deleted.setDescription("Weekly groceries");
        deleted.setCreatedAt(DAY.atTime(13, 0));
        var original = Mappers.getMapper(TransactionMapper.class).from(deleted);
        given(repository.findByIdAndUserIdAndActiveFalse(7L, USER_ID)).willReturn(Optional.of(deleted));

        //when
        var response = service.restoreAndGetSoftDeletedTransaction(7L, USER_ID);

        //then
        assertThat(deleted.getActive()).isTrue();
        assertThat(response).isEqualTo(original);
        then(repository).should().findByIdAndUserIdAndActiveFalse(7L, USER_ID);
    }

    @Test
    @DisplayName("Restore reports not found when no eligible transaction exists")
    void givenNoEligibleTransaction_whenRestored_thenNotFoundWithoutSaving() {
        //given
        given(repository.findByIdAndUserIdAndActiveFalse(7L, USER_ID)).willReturn(Optional.empty());

        //when
        Throwable error = catchThrowable(() -> service.restoreAndGetSoftDeletedTransaction(7L, USER_ID));

        //then
        assertThat(error).isInstanceOf(TransactionNotFoundException.class);
        then(repository).should().findByIdAndUserIdAndActiveFalse(7L, USER_ID);
        then(repository).shouldHaveNoMoreInteractions();
    }


    @ParameterizedTest
    @CsvSource({"date,desc,occurredAt,DESC", "occurredAt,asc,occurredAt,ASC", "amount,ASC,amount,ASC"})
    @DisplayName("Deleted history maps the requested page and uses a stable secondary ID sort")
    void givenDeletedHistoryQuery_whenRequested_thenPageAndSortArePreserved(
            String sortBy, String direction, String property, Sort.Direction expectedDirection) {
        //given
        var query = new TransactionHistoryQuery(USER_ID, EXPENSE, GROCERIES, DAY, DAY, 1, 2, sortBy, direction);
        var pageable = PageRequest.of(1, 2, Sort.by(new Sort.Order(expectedDirection, property), Sort.Order.desc("id")));
        Transaction deleted = groceries();
        deleted.setId(7L);
        deleted.setActive(false);
        given(repository.findAll(org.mockito.ArgumentMatchers.<Specification<Transaction>>any(), eq(pageable)))
                .willReturn(new PageImpl<>(java.util.List.of(deleted), pageable, 3));

        //when
        var response = service.findSoftDeletedTransactions(query);

        //then
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.content()).singleElement().satisfies(transaction -> {
            assertThat(transaction.id()).isEqualTo(7L);
            assertThat(transaction.userId()).isEqualTo(USER_ID);
            assertThat(transaction.amount()).isEqualByComparingTo("10.50");
            assertThat(transaction.currency()).isEqualTo(USD);
        });
        then(repository).should().findAll(org.mockito.ArgumentMatchers.<Specification<Transaction>>any(), eq(pageable));
        then(repository).shouldHaveNoMoreInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"range", "sort", "direction"})
    @DisplayName("Invalid deleted-history queries fail before accessing the repository")
    void givenInvalidDeletedHistoryQuery_whenRequested_thenRejectedBeforeQuerying(String invalidField) {
        //given
        var query = new TransactionHistoryQuery(USER_ID, null, null,
                invalidField.equals("range") ? DAY.plusDays(1) : DAY, DAY, 0, 20,
                invalidField.equals("sort") ? "description" : "date",
                invalidField.equals("direction") ? "sideways" : "desc");

        //when
        Throwable error = catchThrowable(() -> service.findSoftDeletedTransactions(query));

        //then
        assertThat(error).isInstanceOf(InvalidTransactionHistoryQueryException.class);
        then(repository).shouldHaveNoInteractions();
    }

}
