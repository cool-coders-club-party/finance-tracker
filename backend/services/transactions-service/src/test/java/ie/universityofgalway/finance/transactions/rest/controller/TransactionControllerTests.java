package ie.universityofgalway.finance.transactions.rest.controller;

import ie.universityofgalway.finance.transactions.exception.ExchangeRateUnavailableException;
import ie.universityofgalway.finance.transactions.exception.TransactionNotFoundException;
import ie.universityofgalway.finance.transactions.rest.dto.*;
import ie.universityofgalway.finance.transactions.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static ie.universityofgalway.finance.transactions.util.DataUtils.USER_ID;
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
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(ie.universityofgalway.finance.transactions.config.SecurityConfig.class)
class TransactionControllerTests {
    private static final String API = "/api/v1/transactions";
    @Autowired MockMvc mvc;
    @MockitoBean TransactionService transactions;
    @MockitoBean TransactionSummaryService summaries;

    @Test
    @DisplayName("POST accepts a currency and returns the saved transaction with status 201")
    void givenValidBody_whenCreated_thenReturnsSavedTransaction() throws Exception {
        //given
        var request = new CreateTransactionRequest( EXPENSE, GROCERIES, USD,
                new BigDecimal("10.50"), null, DAY.atStartOfDay());
        var saved = new TransactionResponseDto(9L, USER_ID, EXPENSE, GROCERIES, USD, request.amount(),
                null, request.occurredAt(), DAY.atTime(12, 0));
        given(transactions.createTransaction(request, USER_ID)).willReturn(saved);
        //when
        var result = mvc.perform(post(API).with(jwt().jwt(token -> token.subject(USER_ID.toString()))).contentType(APPLICATION_JSON).content("""
                {"transactionType":"EXPENSE","transactionCategory":"GROCERIES",
                 "currency":"USD","amount":"10.50","occurredAt":"2025-03-10T00:00:00"}
                """));
        //then
        result.andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.currency").value("USD")).andExpect(jsonPath("$.amount").value(10.5));
    }

    @Test
    @DisplayName("Missing currency is reported as a field validation error")
    void givenMissingCurrency_whenCreated_thenReturnsBadRequest() throws Exception {
        //given
        String body = """
                {"transactionType":"EXPENSE","transactionCategory":"GROCERIES",
                 "amount":"10.50","occurredAt":"2025-03-10T00:00:00"}
                """;
        //when
        var result = mvc.perform(post(API).with(jwt().jwt(token -> token.subject(USER_ID.toString()))).contentType(APPLICATION_JSON).content(body));
        //then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.currency").isString());
        then(transactions).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("History uses the token owner despite a supplied user ID")
    void givenHistoryFilters_whenRequested_thenReturnsPage() throws Exception {
        //given
        var expected = new TransactionHistoryQuery(USER_ID, EXPENSE, GROCERIES, DAY, null, 0, 20, "date", "desc");
        given(transactions.findHistory(expected)).willReturn(new TransactionPageResponseDto(List.of(), 0, 20, 0, 0));
        //when
        var result = mvc.perform(get(API).with(jwt().jwt(token -> token.subject(USER_ID.toString()))).param("userId","999").param("type","EXPENSE")
                .param("category","GROCERIES").param("from", DAY.toString()));
        //then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("An unknown transaction returns a structured 404")
    void givenUnknownId_whenRead_thenReturnsNotFound() throws Exception {
        //given
        given(transactions.findTransactionById(9L, USER_ID)).willThrow(new TransactionNotFoundException(9L));
        //when
        var result = mvc.perform(get(API + "/9").with(jwt().jwt(token -> token.subject(USER_ID.toString()))));
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
        var result = mvc.perform(patch(API + "/9").with(jwt().jwt(token -> token.subject(USER_ID.toString()))).contentType(APPLICATION_JSON).content(body));
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
        var result = mvc.perform(delete(API + "/{id}", id).with(jwt().jwt(token -> token.subject(USER_ID.toString()))));
        //then
        result.andExpect(status().isOk());
        then(transactions).should().softDelete(id, USER_ID);
        then(transactions).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Permanent deletion requires the explicit request parameter")
    void givenPermanentFlag_whenDeleted_thenHardDeleteIsUsed() throws Exception {
        //given
        long id = 9L;
        //when
        var result = mvc.perform(delete(API + "/{id}", id).with(jwt().jwt(token -> token.subject(USER_ID.toString()))).param("permanent", "true"));
        //then
        result.andExpect(status().isOk());
        then(transactions).should().hardDelete(id, USER_ID);
        then(transactions).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Summary uses the token owner despite a supplied user ID")
    void givenDateRange_whenSummaryRequested_thenReturnsCategoryAmounts() throws Exception {
        //given
        var amount = new BigDecimal("9.50");
        given(summaries.getTransactionSummary(USER_ID, EUR, DAY, DAY)).willReturn(new TransactionSummaryResponseDto(
                EUR, BigDecimal.ZERO, amount, amount.negate(), List.of(new CategorySpendingResponseDto(GROCERIES, amount))));
        //when
        var result = mvc.perform(get(API + "/summary").with(jwt().jwt(token -> token.subject(USER_ID.toString()))).param("userId", "999").param("currency", "EUR")
                .param("from", DAY.toString()).param("to", DAY.toString()));
        //then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.netIncome").value(-9.5))
                .andExpect(jsonPath("$.spendingByCategory[0].category").value("GROCERIES"))
                .andExpect(jsonPath("$.spendingByCategory[0].amount").value(9.5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"currency=XYZ", "currency=EUR&from=invalid", ""})
    @DisplayName("Invalid summary parameters produce a structured 400")
    void givenInvalidParameters_whenSummaryRequested_thenReturnsBadRequest(String query) throws Exception {
        //given
        String uri = API + "/summary?" + query;
        //when
        var result = mvc.perform(get(uri).with(jwt().jwt(token -> token.subject(USER_ID.toString()))));
        //then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(API + "/summary")).andExpect(jsonPath("$.details").isNotEmpty());
        then(summaries).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("An exchange-rate outage produces a 503 response")
    void givenUnavailableRate_whenSummaryRequested_thenReturnsServiceUnavailable() throws Exception {
        //given
        given(summaries.getTransactionSummary(USER_ID, EUR, null, null))
                .willThrow(new ExchangeRateUnavailableException("upstream failure"));
        //when
        var result = mvc.perform(get(API + "/summary").with(jwt().jwt(token -> token.subject(USER_ID.toString()))).param("userId","999").param("currency","EUR"));
        //then
        result.andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Exchange rates are temporarily unavailable."));
    }

    @Test
    @DisplayName("Restore returns the transaction and uses the token owner despite a supplied user ID")
    void givenDeletedTransaction_whenRestored_thenReturnsTransactionForTokenOwner() throws Exception {
        //given
        var restored = new TransactionResponseDto(9L, USER_ID, EXPENSE, GROCERIES, USD,
                new BigDecimal("10.50"), "Groceries", DAY.atStartOfDay(), DAY.atTime(12, 0));
        given(transactions.restoreAndGetSoftDeletedTransaction(9L, USER_ID)).willReturn(restored);

        //when
        var result = mvc.perform(post(API + "/9/restore")
                .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                .param("userId", ie.universityofgalway.finance.transactions.util.DataUtils.OTHER_USER_ID.toString()));

        //then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.amount").value(10.5));
        then(transactions).should().restoreAndGetSoftDeletedTransaction(9L, USER_ID);
        then(transactions).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("An ineligible restore returns a structured 404")
    void givenNoEligibleTransaction_whenRestored_thenReturnsNotFound() throws Exception {
        //given
        given(transactions.restoreAndGetSoftDeletedTransaction(9L, USER_ID))
                .willThrow(new TransactionNotFoundException(9L));

        //when
        var result = mvc.perform(post(API + "/9/restore")
                .with(jwt().jwt(token -> token.subject(USER_ID.toString()))));

        //then
        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(API + "/9/restore"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "invalid"})
    @DisplayName("Invalid restore IDs are rejected before calling the service")
    void givenInvalidId_whenRestored_thenReturnsBadRequest(String id) throws Exception {
        //given
        String uri = API + "/" + id + "/restore";

        //when
        var result = mvc.perform(post(uri).with(jwt().jwt(token -> token.subject(USER_ID.toString()))));

        //then
        result.andExpect(status().isBadRequest());
        then(transactions).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Restore requires authentication")
    void givenNoToken_whenRestored_thenReturnsUnauthorized() throws Exception {
        //given
        String uri = API + "/9/restore";

        //when
        var result = mvc.perform(post(uri));

        //then
        result.andExpect(status().isUnauthorized());
        then(transactions).shouldHaveNoInteractions();
    }


    @Test
    @DisplayName("Deleted history forwards filters and pagination using only the token owner")
    void givenDeletedHistoryFiltersAndForgedOwner_whenRequested_thenTokenOwnerAndPageAreUsed() throws Exception {
        //given
        var query = new TransactionHistoryQuery(USER_ID, EXPENSE, GROCERIES, DAY, DAY, 1, 2, "amount", "asc");
        var record = new TransactionResponseDto(9L, USER_ID, EXPENSE, GROCERIES, USD,
                new BigDecimal("10.50"), "Groceries", DAY.atStartOfDay(), DAY.atTime(12, 0));
        given(transactions.findSoftDeletedTransactions(query))
                .willReturn(new TransactionPageResponseDto(List.of(record), 1, 2, 3, 2));

        //when
        var result = mvc.perform(get(API + "/deleted")
                .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                .param("userId", ie.universityofgalway.finance.transactions.util.DataUtils.OTHER_USER_ID.toString())
                .param("type", "EXPENSE").param("category", "GROCERIES")
                .param("from", DAY.toString()).param("to", DAY.toString())
                .param("page", "1").param("size", "2").param("sortBy", "amount").param("direction", "asc"));

        //then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2)).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(9))
                .andExpect(jsonPath("$.content[0].userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.content[0].currency").value("USD"));
        then(transactions).should().findSoftDeletedTransactions(query);
        then(transactions).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Deleted history uses defaults and returns an empty page when nothing matches")
    void givenNoDeletedTransactions_whenRequestedWithoutFilters_thenReturnsEmptyDefaultPage() throws Exception {
        //given
        var query = new TransactionHistoryQuery(USER_ID, null, null, null, null, 0, 20, "date", "desc");
        given(transactions.findSoftDeletedTransactions(query))
                .willReturn(new TransactionPageResponseDto(List.of(), 0, 20, 0, 0));

        //when
        var result = mvc.perform(get(API + "/deleted").with(jwt().jwt(token -> token.subject(USER_ID.toString()))));

        //then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.totalPages").value(0));
        then(transactions).should().findSoftDeletedTransactions(query);
        then(transactions).shouldHaveNoMoreInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "type=UNKNOWN", "category=UNKNOWN", "from=invalid", "to=invalid"})
    @DisplayName("Invalid deleted-history parameters return 400 without calling the service")
    void givenInvalidDeletedHistoryParameters_whenRequested_thenBadRequest(String query) throws Exception {
        //given
        String uri = API + "/deleted?" + query;

        //when
        var result = mvc.perform(get(uri).with(jwt().jwt(token -> token.subject(USER_ID.toString()))));

        //then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(API + "/deleted"));
        then(transactions).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Deleted history requires authentication")
    void givenNoToken_whenDeletedHistoryRequested_thenUnauthorized() throws Exception {
        //given
        String uri = API + "/deleted";

        //when
        var result = mvc.perform(get(uri));

        //then
        result.andExpect(status().isUnauthorized());
        then(transactions).shouldHaveNoInteractions();
    }

}
