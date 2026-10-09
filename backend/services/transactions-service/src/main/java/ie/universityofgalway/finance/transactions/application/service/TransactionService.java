package ie.universityofgalway.finance.transactions.application.service;

import ie.universityofgalway.finance.transactions.api.web.dto.CreateTransactionRequest;
import ie.universityofgalway.finance.transactions.api.web.dto.TransactionPageResponseDto;
import ie.universityofgalway.finance.transactions.api.web.dto.TransactionResponseDto;
import ie.universityofgalway.finance.transactions.api.web.dto.UpdateTransactionRequest;

public interface TransactionService {
    TransactionResponseDto createTransaction(CreateTransactionRequest request);

    TransactionPageResponseDto findHistory(TransactionHistoryQuery query);

    TransactionResponseDto findTransactionById(Long transactionId);

    TransactionResponseDto updateTransaction(Long transactionId, UpdateTransactionRequest request);
}
