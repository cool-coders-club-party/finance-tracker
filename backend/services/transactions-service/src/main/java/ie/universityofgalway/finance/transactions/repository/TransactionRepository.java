package ie.universityofgalway.finance.transactions.repository;

import ie.universityofgalway.finance.transactions.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("""
            FROM Transaction i WHERE i.id = :id AND i.active = true
            """)
    Optional<Transaction> findActiveById(@Param("id") Long transactionId);

    @Modifying
    @Query("""
            UPDATE Transaction i SET i.active = false WHERE i.id = :id AND i.active = true
            """)
    Integer softDelete(@Param("id") Long transactionId);


    @Modifying
    @Query("""
            DELETE Transaction i WHERE i.id = :id
            """)
    Integer hardDelete(@Param("id")Long transactionId);
}
