package ie.universityofgalway.finance.transactions.application.service;

import ie.universityofgalway.finance.transactions.api.web.dto.CreateTransactionRequest;
import ie.universityofgalway.finance.transactions.api.web.dto.TransactionResponse;
import ie.universityofgalway.finance.transactions.domain.Transaction;
import ie.universityofgalway.finance.transactions.infrastructure.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setUserId(request.userId());
        transaction.setTransactionType(request.transactionType());
        transaction.setTransactionCategory(request.transactionCategory());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setOccurredAt(request.occurredAt());

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> findByUserId(Long userId) {
        return transactionRepository.findByUserIdOrderByOccurredAtDesc(userId)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
