package ie.universityofgalway.finance.transactions.infrastructure.repository;

import ie.universityofgalway.finance.transactions.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
}
