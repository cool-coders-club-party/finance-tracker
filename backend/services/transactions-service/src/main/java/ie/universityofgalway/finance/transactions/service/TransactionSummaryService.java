package ie.universityofgalway.finance.transactions.service;

import ie.universityofgalway.finance.transactions.currency.CurrencyConversionService;
import ie.universityofgalway.finance.transactions.entity.Transaction;
import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import ie.universityofgalway.finance.transactions.entity.TransactionType;
import ie.universityofgalway.finance.transactions.exception.InvalidTransactionHistoryQueryException;
import ie.universityofgalway.finance.transactions.repository.TransactionRepository;
import ie.universityofgalway.finance.transactions.repository.TransactionSpecifications;
import ie.universityofgalway.finance.transactions.rest.dto.CategoryIncomeResponseDto;
import ie.universityofgalway.finance.transactions.rest.dto.CategorySpendingResponseDto;
import ie.universityofgalway.finance.transactions.rest.dto.TransactionSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Summarizes all matching active transactions, independently of history pagination.
 * Original amounts remain unchanged; each is converted using its occurrence date.
 * A required rate failure propagates rather than producing an incomplete summary.
 */
@Service
@RequiredArgsConstructor
public class TransactionSummaryService {

    private final TransactionRepository transactionRepository;
    private final CurrencyConversionService currencyConversionService;

    /**
     * Calculates converted income, expenses and net income for one owner.
     * Unrounded converted amounts accumulate separately per income and expense category.
     * Each category is rounded to two decimals using HALF_EVEN, then its rounded amount
     * contributes to the corresponding total so both breakdowns reconcile.
     * Net income is total income minus total expenses, not an account balance.
     *
     * @param userId         authenticated owner UUID
     * @param targetCurrency currency of every returned monetary total
     * @param from           optional inclusive first day
     * @param to             optional inclusive last day
     * @return totals and both category breakdowns; zero totals and empty lists when nothing matches
     * @throws InvalidTransactionHistoryQueryException if from follows to
     * @throws ie.universityofgalway.finance.transactions.exception.ExchangeRateUnavailableException if a required rate is unavailable
     */
    public TransactionSummaryResponseDto getTransactionSummary(
            UUID userId,
            TransactionCurrency targetCurrency,
            LocalDate from,
            LocalDate to
    ) {
        validateDateRange(from, to);
        SummaryTotals totals = new SummaryTotals();
        for (Transaction transaction : getActiveTransactions(userId, from, to)) {
            BigDecimal amount = convertAmount(transaction, targetCurrency);
            totals.record(transaction, amount);
        }
        return totals.toResponse(targetCurrency);
    }

    private static void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidTransactionHistoryQueryException("from must be on or before to.");
        }
    }

    private BigDecimal convertAmount(Transaction transaction, TransactionCurrency targetCurrency) {
        return currencyConversionService.convert(
                transaction.getAmount(),
                transaction.getCurrency(),
                targetCurrency,
                transaction.getOccurredAt().toLocalDate()
        );
    }

    private List<Transaction> getActiveTransactions(UUID userId, LocalDate from, LocalDate to) {
        return transactionRepository.findAll(
                TransactionSpecifications.forHistory(userId, null, null, from, to)
        );
    }

    private static final class SummaryTotals {
        private final Map<TransactionCategory, BigDecimal> expensesByCategory =
                new EnumMap<>(TransactionCategory.class);
        private final Map<TransactionCategory, BigDecimal> incomesByCategory =
                new EnumMap<>(TransactionCategory.class);

        void record(Transaction transaction, BigDecimal convertedAmount) {
            if (transaction.getTransactionType().equals(TransactionType.INCOME)) {
                incomesByCategory.merge(
                        transaction.getTransactionCategory(),
                        convertedAmount,
                        BigDecimal::add
                );
            } else {
                expensesByCategory.merge(
                        transaction.getTransactionCategory(),
                        convertedAmount,
                        BigDecimal::add
                );
            }
        }

        TransactionSummaryResponseDto toResponse(TransactionCurrency currency) {
            List<CategorySpendingResponseDto> categories = getRoundedCategorySpending();
            List<CategoryIncomeResponseDto> incomes = getRoundedCategoryIncomes();
            BigDecimal totalIncome = incomes.stream()
                    .map(CategoryIncomeResponseDto::amount)
                    .reduce(round(BigDecimal.ZERO), BigDecimal::add);
            BigDecimal totalExpenses = categories.stream()
                    .map(CategorySpendingResponseDto::amount)
                    .reduce(round(BigDecimal.ZERO), BigDecimal::add);
            return new TransactionSummaryResponseDto(
                    currency,
                    totalIncome,
                    totalExpenses,
                    totalIncome.subtract(totalExpenses),
                    categories,
                    incomes
            );
        }

        private List<CategoryIncomeResponseDto> getRoundedCategoryIncomes() {
            List<CategoryIncomeResponseDto> incomeCategories = new ArrayList<>();
            incomesByCategory.forEach(
                    (category, amount) ->
                            incomeCategories.add(
                                    new CategoryIncomeResponseDto(
                                            category,
                                            round(amount)
                                    )
                            )
            );
            return incomeCategories;
        }

        private List<CategorySpendingResponseDto> getRoundedCategorySpending() {
            List<CategorySpendingResponseDto> expensesCategories = new ArrayList<>();
            expensesByCategory.forEach(
                    (category, amount) ->
                            expensesCategories.add(
                                    new CategorySpendingResponseDto(
                                            category,
                                            round(amount)
                                    )
                            )
            );
            return expensesCategories;
        }

        private static BigDecimal round(BigDecimal amount) {
            return amount.setScale(2, RoundingMode.HALF_EVEN);
        }
    }
}
