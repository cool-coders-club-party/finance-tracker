package ie.universityofgalway.finance.transactions.service;

import ie.universityofgalway.finance.transactions.currency.CurrencyConversionService;
import ie.universityofgalway.finance.transactions.entity.Transaction;
import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import ie.universityofgalway.finance.transactions.entity.TransactionType;
import ie.universityofgalway.finance.transactions.exception.InvalidTransactionHistoryQueryException;
import ie.universityofgalway.finance.transactions.repository.TransactionRepository;
import ie.universityofgalway.finance.transactions.repository.TransactionSpecifications;
import ie.universityofgalway.finance.transactions.rest.dto.CategorySpendingResponseDto;
import ie.universityofgalway.finance.transactions.rest.dto.TransactionSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionSummaryService {

    private final TransactionRepository transactionRepository;
    private final CurrencyConversionService currencyConversionService;

    public TransactionSummaryResponseDto getTransactionSummary(
            Long userId,
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

    private List<Transaction> getActiveTransactions(Long userId, LocalDate from, LocalDate to) {
        return transactionRepository.findAll(
                TransactionSpecifications.forHistory(userId, null, null, from, to)
        );
    }

    private static final class SummaryTotals {
        private BigDecimal income = BigDecimal.ZERO;
        private final Map<TransactionCategory, BigDecimal> expensesByCategory =
                new EnumMap<>(TransactionCategory.class);

        void record(Transaction transaction, BigDecimal convertedAmount) {
            if (transaction.getTransactionType().equals(TransactionType.INCOME)) {
                income = income.add(convertedAmount);
            } else {
                expensesByCategory.merge(
                        transaction.getTransactionCategory(),
                        convertedAmount,
                        BigDecimal::add
                );
            }
        }

        TransactionSummaryResponseDto toResponse(TransactionCurrency currency) {
            List<CategorySpendingResponseDto> categories = roundedCategorySpending();
            BigDecimal totalIncome = round(income);
            BigDecimal totalExpenses = categories.stream()
                    .map(CategorySpendingResponseDto::amount)
                    .reduce(round(BigDecimal.ZERO), BigDecimal::add);
            return new TransactionSummaryResponseDto(
                    currency, totalIncome, totalExpenses, totalIncome.subtract(totalExpenses), categories
            );
        }

        private List<CategorySpendingResponseDto> roundedCategorySpending() {
            List<CategorySpendingResponseDto> categories = new ArrayList<>();
            expensesByCategory.forEach((category, amount) ->
                    categories.add(new CategorySpendingResponseDto(category, round(amount))));
            return categories;
        }

        private static BigDecimal round(BigDecimal amount) {
            return amount.setScale(2, RoundingMode.HALF_EVEN);
        }
    }
}
