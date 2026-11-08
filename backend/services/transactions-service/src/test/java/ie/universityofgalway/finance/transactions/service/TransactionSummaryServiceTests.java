package ie.universityofgalway.finance.transactions.service;

import ie.universityofgalway.finance.transactions.currency.CurrencyConversionService;
import ie.universityofgalway.finance.transactions.currency.ExchangeRate;
import ie.universityofgalway.finance.transactions.currency.ExchangeRateProvider;
import ie.universityofgalway.finance.transactions.entity.Transaction;
import ie.universityofgalway.finance.transactions.exception.ExchangeRateUnavailableException;
import ie.universityofgalway.finance.transactions.exception.InvalidTransactionHistoryQueryException;
import ie.universityofgalway.finance.transactions.repository.TransactionRepository;
import ie.universityofgalway.finance.transactions.rest.dto.CategorySpendingResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static ie.universityofgalway.finance.transactions.entity.TransactionCategory.*;
import static ie.universityofgalway.finance.transactions.entity.TransactionCurrency.*;
import static ie.universityofgalway.finance.transactions.entity.TransactionType.INCOME;
import static ie.universityofgalway.finance.transactions.util.DataUtils.*;
import static org.assertj.core.api.Assertions.*;
import org.mockito.ArgumentMatchers;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionSummaryServiceTests {
    @Mock TransactionRepository repository;
    @Mock ExchangeRateProvider rates;
    TransactionSummaryService service;

    @BeforeEach
    void setUp() {
        service = new TransactionSummaryService(repository, new CurrencyConversionService(rates));
    }

    @Test
    @DisplayName("Historical conversion rounds once per category and reconciles with net income")
    void givenMixedTransactions_whenSummarized_thenRoundedCategoriesReconcile() {
        //given
        var first = groceries();
        first.setAmount(new BigDecimal("1.00"));
        var second = groceries();
        second.setAmount(new BigDecimal("1.00"));
        second.setOccurredAt(DAY.plusDays(1).atStartOfDay());
        var dining = groceries();
        dining.setTransactionCategory(DINING);
        dining.setAmount(new BigDecimal("1.00"));
        var salary = groceries();
        salary.setCurrency(EUR);
        salary.setTransactionType(INCOME);
        salary.setTransactionCategory(SALARY);
        salary.setAmount(new BigDecimal("10.00"));
        given(repository.findAll(ArgumentMatchers.<Specification<Transaction>>any()))
                .willReturn(List.of(first, second, dining, salary));
        given(rates.getRate(USD, EUR, DAY)).willReturn(new ExchangeRate(USD, EUR, DAY, new BigDecimal("0.905")));
        given(rates.getRate(USD, EUR, DAY.plusDays(1))).willReturn(
                new ExchangeRate(USD, EUR, DAY.plusDays(1), new BigDecimal("0.915")));
        given(rates.getRate(EUR, EUR, DAY)).willReturn(new ExchangeRate(EUR, EUR, DAY, BigDecimal.ONE));
        //when
        var summary = service.getTransactionSummary(USER_ID, EUR, null, null);
        //then
        assertThat(summary.spendingByCategory()).containsExactlyInAnyOrder(
                new CategorySpendingResponseDto(GROCERIES, new BigDecimal("1.82")),
                new CategorySpendingResponseDto(DINING, new BigDecimal("0.90")));
        assertThat(summary.expenses()).isEqualByComparingTo("2.72");
        assertThat(summary.income()).isEqualByComparingTo("10.00");
        assertThat(summary.netIncome()).isEqualByComparingTo("7.28");
        assertThat(first.getAmount()).isEqualByComparingTo("1.00");
        assertThat(first.getCurrency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("An empty date range returns zero totals in the requested currency")
    void givenNoTransactions_whenSummarized_thenTotalsAreZero() {
        //given
        given(repository.findAll(ArgumentMatchers.<Specification<Transaction>>any())).willReturn(List.of());
        //when
        var summary = service.getTransactionSummary(USER_ID, GBP, DAY, DAY);
        //then
        assertThat(summary.currency()).isEqualTo(GBP);
        assertThat(List.of(summary.income(), summary.expenses(), summary.netIncome()))
                .containsOnly(new BigDecimal("0.00"));
        assertThat(summary.spendingByCategory()).isEmpty();
        then(rates).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("A reversed range is rejected before any data is fetched")
    void givenReversedDates_whenSummarized_thenInvalidQueryIsRaised() {
        //given
        var end = DAY.minusDays(1);
        //when
        Throwable error = catchThrowable(() -> service.getTransactionSummary(USER_ID, EUR, DAY, end));
        //then
        assertThat(error).isInstanceOf(InvalidTransactionHistoryQueryException.class);
        then(repository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("An unavailable rate prevents a partial summary")
    void givenUnavailableRate_whenSummarized_thenErrorPropagates() {
        //given
        given(repository.findAll(ArgumentMatchers.<Specification<Transaction>>any()))
                .willReturn(List.of(groceries()));
        given(rates.getRate(USD, EUR, DAY)).willThrow(new ExchangeRateUnavailableException("Rate unavailable"));
        //when
        Throwable error = catchThrowable(() -> service.getTransactionSummary(USER_ID, EUR, null, null));
        //then
        assertThat(error).isInstanceOf(ExchangeRateUnavailableException.class);
    }
}
