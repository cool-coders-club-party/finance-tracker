package ie.universityofgalway.finance.transactions.service;

import ie.universityofgalway.finance.transactions.rest.dto.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Owns transaction lifecycle operations and frontend metadata.
 * Callers must obtain the owner UUID from trusted authentication, not a request body.
 * Owner-scoped lookups report missing and other users' records identically.
 */
public interface TransactionService {
    /**
     * Creates an active transaction for the authenticated owner after validating its type/category combination.
     * The supplied amount and currency are recorded without conversion.
     *
     * @param request validated transaction fields
     * @param userId authenticated owner UUID
     * @return the persisted transaction, including its generated ID
     */
    TransactionResponseDto createTransaction(CreateTransactionRequest request, UUID userId);

    /**
     * Returns a page of the owner's active transactions matching the optional filters.
     * Date bounds include both calendar days; page numbering starts at zero.
     *
     * @param query owner, filters, pagination and sorting
     * @return matching active records and page totals
     */
    TransactionPageResponseDto findHistory(TransactionHistoryQuery query);

    /**
     * Finds an active transaction belonging to the authenticated owner.
     *
     * @param transactionId transaction identifier
     * @param userId authenticated owner UUID
     * @return the matching transaction
     * @throws ie.universityofgalway.finance.transactions.exception.TransactionNotFoundException if absent, inactive or owned by someone else
     */
    TransactionResponseDto findTransactionById(Long transactionId, UUID userId);

    /**
     * Applies non-null fields to an owned active transaction. The resulting type/category
     * combination is validated before mapping. Changing currency does not convert the amount.
     *
     * @param transactionId transaction identifier
     * @param request validated partial update; null fields preserve existing values
     * @param userId authenticated owner UUID
     * @return the updated transaction
     * @throws ie.universityofgalway.finance.transactions.exception.TransactionNotFoundException if no owned active record exists
     */
    TransactionResponseDto updateTransaction(Long transactionId, UpdateTransactionRequest request, UUID userId);

    /**
     * Marks an owned active transaction inactive, excluding it from normal history and summaries.
     *
     * @param transactionId transaction identifier
     * @param userId authenticated owner UUID
     * @throws ie.universityofgalway.finance.transactions.exception.TransactionNotFoundException if no owned active record exists
     */
    void softDelete(Long transactionId, UUID userId);

    /**
     * Permanently removes an owned transaction, whether active or already soft-deleted.
     * The removed record cannot be restored.
     *
     * @param transactionId transaction identifier
     * @param userId authenticated owner UUID
     * @throws ie.universityofgalway.finance.transactions.exception.TransactionNotFoundException if no owned record exists
     */
    void hardDelete(Long transactionId, UUID userId);

    /**
     * Derives available types, category/type relationships and currencies from domain enums.
     *
     * @return options for frontend forms and filters
     */
    TransactionMetadataResponseDto findTransactionMetadata();

    /**
     * Reactivates an owned soft-deleted record without replacing its original transaction fields.
     *
     * @param transactionId transaction identifier
     * @param userId authenticated owner UUID
     * @return the restored transaction
     * @throws ie.universityofgalway.finance.transactions.exception.TransactionNotFoundException if absent, active or owned by someone else
     */
    TransactionResponseDto restoreAndGetSoftDeletedTransaction(Long transactionId, UUID userId);

    /**
     * Returns only the owner's inactive transactions using the same filtering and pagination
     * semantics as active history.
     *
     * @param query owner, filters, pagination and sorting
     * @return matching deleted records and page totals
     */
    TransactionPageResponseDto findSoftDeletedTransactions(TransactionHistoryQuery query);
}
