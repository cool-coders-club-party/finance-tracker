package ie.universityofgalway.finance.transactions.repository;

import ie.universityofgalway.finance.transactions.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("""
            FROM Transaction i WHERE i.id = :id AND i.userId = :userId AND i.active = true
            """)
    Optional<Transaction> findActiveByIdAndUserId(@Param("id") Long transactionId, @Param("userId") UUID userId);

    @Modifying
    @Query("""
            UPDATE Transaction i SET i.active = false WHERE i.id = :id AND i.userId = :userId AND i.active = true
            """)
    Integer softDelete(@Param("id") Long transactionId, @Param("userId") UUID userId);


    @Modifying
    @Query("""
            DELETE Transaction i WHERE i.id = :id AND i.userId = :userId
            """)
    Integer hardDelete(@Param("id") Long transactionId, @Param("userId") UUID userId);
}
