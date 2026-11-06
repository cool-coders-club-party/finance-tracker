package ie.universityofgalway.finance.transactions.service;

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
        var request = new CreateTransactionRequest(42L, EXPENSE, GROCERIES, GBP,
                new BigDecimal("12.34"), "Lunch", DAY.atTime(12, 0));
        given(repository.save(any(Transaction.class))).willAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(17L);
            return saved;
        });
        //when
        var response = service.createTransaction(request);
        //then
        assertThat(response.id()).isEqualTo(17L);
        assertThat(response.currency()).isEqualTo(GBP);
        assertThat(response.amount()).isEqualByComparingTo("12.34");
    }

    @Test
    @DisplayName("Changing only the denomination preserves the original amount")
    void givenUsdExpense_whenCurrencyPatched_thenOnlyCurrencyChanges() {
        //given
        given(repository.findActiveById(7L)).willReturn(Optional.of(groceries()));
        var patch = new UpdateTransactionRequest(null, null, null, EUR, null, null);
        //when
        var response = service.updateTransaction(7L, patch);
        //then
        assertThat(response.currency()).isEqualTo(EUR);
        assertThat(response.amount()).isEqualByComparingTo("10.50");
        assertThat(response.transactionCategory()).isEqualTo(GROCERIES);
    }

    @Test
    @DisplayName("An amount-only patch preserves the saved currency")
    void givenUsdExpense_whenAmountPatched_thenCurrencyIsPreserved() {
        //given
        given(repository.findActiveById(7L)).willReturn(Optional.of(groceries()));
        var patch = new UpdateTransactionRequest(new BigDecimal("20.00"), null, null, null, null, null);
        //when
        var response = service.updateTransaction(7L, patch);
        //then
        assertThat(response.amount()).isEqualByComparingTo("20");
        assertThat(response.currency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("A type-only patch must be compatible with the existing category")
    void givenGroceriesExpense_whenChangedToIncome_thenPatchIsRejected() {
        //given
        var saved = groceries();
        given(repository.findActiveById(7L)).willReturn(Optional.of(saved));
        var patch = new UpdateTransactionRequest(null, null, null, null, INCOME, null);
        //when
        Throwable error = catchThrowable(() -> service.updateTransaction(7L, patch));
        //then
        assertThat(error).isInstanceOf(InvalidTransactionCombinationException.class);
        assertThat(saved.getTransactionType()).isEqualTo(EXPENSE);
    }

    @Test
    @DisplayName("A missing active transaction is reported as not found")
    void givenNoActiveTransaction_whenRead_thenNotFound() {
        //given
        given(repository.findActiveById(7L)).willReturn(Optional.empty());
        //when
        Throwable error = catchThrowable(() -> service.findTransactionById(7L));
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
}
