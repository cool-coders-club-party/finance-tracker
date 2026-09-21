package ie.universityofgalway.finance.transactions.application.service;

import ie.universityofgalway.finance.transactions.api.web.dto.CreateTransactionRequest;
import ie.universityofgalway.finance.transactions.api.web.dto.TransactionResponse;

import java.util.List;

public interface TransactionService {
    TransactionResponse create(CreateTransactionRequest request);

    List<TransactionResponse> findByUserId(Long userId);
}
