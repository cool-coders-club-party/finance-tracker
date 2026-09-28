package ie.universityofgalway.finance.transactions.infrastructure.repository;

import ie.universityofgalway.finance.transactions.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    List<Transaction> findByUserIdOrderByOccurredAtDesc(Long userId);
}
