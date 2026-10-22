package ie.universityofgalway.finance.transactions.service;

import ie.universityofgalway.finance.transactions.rest.dto.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface TransactionService {
    TransactionResponseDto createTransaction(CreateTransactionRequest request);

    TransactionPageResponseDto findHistory(TransactionHistoryQuery query);

    TransactionResponseDto findTransactionById(Long transactionId);

    TransactionResponseDto updateTransaction(Long transactionId, UpdateTransactionRequest request);

    void softDelete(Long transactionId);

    void hardDelete(Long transactionId);

    TransactionMetadataResponseDto findTransactionMetadata();
}
