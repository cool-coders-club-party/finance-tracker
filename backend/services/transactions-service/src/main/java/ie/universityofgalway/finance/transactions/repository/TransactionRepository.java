package ie.universityofgalway.finance.transactions.repository;

import ie.universityofgalway.finance.transactions.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists transactions and supports composable history queries.
 * Lifecycle queries include ownership explicitly; inherited unrestricted repository
 * methods must only be used with appropriate owner-scoping by callers.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("""
            FROM Transaction i WHERE i.id = :id AND i.userId = :userId AND i.active = true
            """)
    Optional<Transaction> findActiveByIdAndUserId(@Param("id") Long transactionId, @Param("userId") UUID userId);

    /**
     * Marks an owned active row inactive. Requires a transaction supplied by the service.
     *
     * @param transactionId transaction identifier
     * @param userId owner UUID
     * @return affected row count, zero when no owned active record matches
     */
    @Modifying
    @Query("""
            UPDATE Transaction i SET i.active = false WHERE i.id = :id AND i.userId = :userId AND i.active = true
            """)
    Integer softDelete(@Param("id") Long transactionId, @Param("userId") UUID userId);


    /**
     * Physically deletes an owned row regardless of its active flag.
     * Requires a transaction supplied by the service.
     *
     * @param transactionId transaction identifier
     * @param userId owner UUID
     * @return affected row count, zero when no owned record matches
     */
    @Modifying
    @Query("""
            DELETE Transaction i WHERE i.id = :id AND i.userId = :userId
            """)
    Integer hardDelete(@Param("id") Long transactionId, @Param("userId") UUID userId);

    Optional<Transaction> findByIdAndUserIdAndActiveFalse(Long transactionId, UUID userId);


}
