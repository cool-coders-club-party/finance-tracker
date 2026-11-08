package ie.universityofgalway.finance.transactions.service;

import ie.universityofgalway.finance.transactions.rest.dto.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public interface TransactionService {
    TransactionResponseDto createTransaction(CreateTransactionRequest request, UUID userId);

    TransactionPageResponseDto findHistory(TransactionHistoryQuery query);

    TransactionResponseDto findTransactionById(Long transactionId, UUID userId);

    TransactionResponseDto updateTransaction(Long transactionId, UpdateTransactionRequest request, UUID userId);

    void softDelete(Long transactionId, UUID userId);

    void hardDelete(Long transactionId, UUID userId);

    TransactionMetadataResponseDto findTransactionMetadata();

    TransactionResponseDto restoreAndGetSoftDeletedTransaction(Long transactionId, UUID userId);

    TransactionPageResponseDto findSoftDeletedTransactions(TransactionHistoryQuery query);
}
