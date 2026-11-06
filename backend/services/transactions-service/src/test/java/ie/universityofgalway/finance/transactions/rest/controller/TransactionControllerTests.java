package ie.universityofgalway.finance.transactions.rest.controller;

import ie.universityofgalway.finance.transactions.exception.ExchangeRateUnavailableException;
import ie.universityofgalway.finance.transactions.exception.TransactionNotFoundException;
import ie.universityofgalway.finance.transactions.rest.dto.*;
import ie.universityofgalway.finance.transactions.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static ie.universityofgalway.finance.transactions.entity.TransactionCategory.GROCERIES;
import static ie.universityofgalway.finance.transactions.entity.TransactionCurrency.*;
import static ie.universityofgalway.finance.transactions.entity.TransactionType.EXPENSE;
import static ie.universityofgalway.finance.transactions.util.DataUtils.DAY;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTests {
    private static final String API = "/api/v1/transactions";
    @Autowired MockMvc mvc;
    @MockitoBean TransactionService transactions;
    @MockitoBean TransactionSummaryService summaries;

    @Test
    @DisplayName("POST accepts a currency and returns the saved transaction with status 201")
    void givenValidBody_whenCreated_thenReturnsSavedTransaction() throws Exception {
        //given
        var request = new CreateTransactionRequest(42L, EXPENSE, GROCERIES, USD,
                new BigDecimal("10.50"), null, DAY.atStartOfDay());
        var saved = new TransactionResponseDto(9L, 42L, EXPENSE, GROCERIES, USD, request.amount(),
                null, request.occurredAt(), DAY.atTime(12, 0));
        given(transactions.createTransaction(request)).willReturn(saved);
        //when
        var result = mvc.perform(post(API).contentType(APPLICATION_JSON).content("""
                {"userId":42,"transactionType":"EXPENSE","transactionCategory":"GROCERIES",
                 "currency":"USD","amount":"10.50","occurredAt":"2025-03-10T00:00:00"}
                """));
        //then
        result.andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.currency").value("USD")).andExpect(jsonPath("$.amount").value(10.5));
    }

    @Test
    @DisplayName("Missing currency is reported as a field validation error")
    void givenMissingCurrency_whenCreated_thenReturnsBadRequest() throws Exception {
        //given
        String body = """
                {"userId":42,"transactionType":"EXPENSE","transactionCategory":"GROCERIES",
                 "amount":"10.50","occurredAt":"2025-03-10T00:00:00"}
                """;
        //when
        var result = mvc.perform(post(API).contentType(APPLICATION_JSON).content(body));
        //then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.currency").isString());
        then(transactions).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("History binds the user, filters and pagination defaults")
    void givenHistoryFilters_whenRequested_thenReturnsPage() throws Exception {
        //given
        var expected = new TransactionHistoryQuery(42L, EXPENSE, GROCERIES, DAY, null, 0, 20, "date", "desc");
        given(transactions.findHistory(expected)).willReturn(new TransactionPageResponseDto(List.of(), 0, 20, 0, 0));
        //when
        var result = mvc.perform(get(API).param("userId","42").param("type","EXPENSE")
                .param("category","GROCERIES").param("from", DAY.toString()));
        //then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("An unknown transaction returns a structured 404")
    void givenUnknownId_whenRead_thenReturnsNotFound() throws Exception {
        //given
        given(transactions.findTransactionById(9L)).willThrow(new TransactionNotFoundException(9L));
        //when
        var result = mvc.perform(get(API + "/9"));
        //then
        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(API + "/9"));
    }

    @Test
    @DisplayName("Empty PATCH bodies are rejected before an update")
    void givenEmptyPatch_whenUpdated_thenReturnsBadRequest() throws Exception {
        //given
        String body = "{}";
        //when
        var result = mvc.perform(patch(API + "/9").contentType(APPLICATION_JSON).content(body));
        //then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Validation failed"));
        then(transactions).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DELETE defaults to soft deletion")
    void givenNoDeleteMode_whenDeleted_thenSoftDeleteIsUsed() throws Exception {
        //given
        long id = 9L;
        //when
        var result = mvc.perform(delete(API + "/{id}", id));
        //then
        result.andExpect(status().isOk());
        then(transactions).should().softDelete(id);
        then(transactions).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Permanent deletion requires the explicit request parameter")
    void givenPermanentFlag_whenDeleted_thenHardDeleteIsUsed() throws Exception {
        //given
        long id = 9L;
        //when
        var result = mvc.perform(delete(API + "/{id}", id).param("permanent", "true"));
        //then
        result.andExpect(status().isOk());
        then(transactions).should().hardDelete(id);
        then(transactions).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Summary binds optional dates and returns the selected currency and categories")
    void givenDateRange_whenSummaryRequested_thenReturnsCategoryAmounts() throws Exception {
        //given
        var amount = new BigDecimal("9.50");
        given(summaries.getTransactionSummary(42L, EUR, DAY, DAY)).willReturn(new TransactionSummaryResponseDto(
                EUR, BigDecimal.ZERO, amount, amount.negate(), List.of(new CategorySpendingResponseDto(GROCERIES, amount))));
        //when
        var result = mvc.perform(get(API + "/summary").param("userId", "42").param("currency", "EUR")
                .param("from", DAY.toString()).param("to", DAY.toString()));
        //then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.netIncome").value(-9.5))
                .andExpect(jsonPath("$.spendingByCategory[0].category").value("GROCERIES"))
                .andExpect(jsonPath("$.spendingByCategory[0].amount").value(9.5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"userId=0&currency=EUR", "userId=42&currency=XYZ", "userId=42"})
    @DisplayName("Invalid summary parameters produce a structured 400")
    void givenInvalidParameters_whenSummaryRequested_thenReturnsBadRequest(String query) throws Exception {
        //given
        String uri = API + "/summary?" + query;
        //when
        var result = mvc.perform(get(uri));
        //then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(API + "/summary")).andExpect(jsonPath("$.details").isNotEmpty());
        then(summaries).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("An exchange-rate outage produces a 503 response")
    void givenUnavailableRate_whenSummaryRequested_thenReturnsServiceUnavailable() throws Exception {
        //given
        given(summaries.getTransactionSummary(42L, EUR, null, null))
                .willThrow(new ExchangeRateUnavailableException("upstream failure"));
        //when
        var result = mvc.perform(get(API + "/summary").param("userId","42").param("currency","EUR"));
        //then
        result.andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Exchange rates are temporarily unavailable."));
    }
}
