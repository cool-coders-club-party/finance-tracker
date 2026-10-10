package ie.universityofgalway.finance.transactions.service;

import ie.universityofgalway.finance.transactions.rest.dto.CreateTransactionRequest;
import ie.universityofgalway.finance.transactions.rest.dto.TransactionPageResponseDto;
import ie.universityofgalway.finance.transactions.rest.dto.TransactionResponseDto;
import ie.universityofgalway.finance.transactions.rest.dto.UpdateTransactionRequest;

public interface TransactionService {
    TransactionResponseDto createTransaction(CreateTransactionRequest request);

    TransactionPageResponseDto findHistory(TransactionHistoryQuery query);

    TransactionResponseDto findTransactionById(Long transactionId);

    TransactionResponseDto updateTransaction(Long transactionId, UpdateTransactionRequest request);
}
