package ie.universityofgalway.finance.transactions.infrastructure.service;

import ie.universityofgalway.finance.transactions.infrastructure.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

}
