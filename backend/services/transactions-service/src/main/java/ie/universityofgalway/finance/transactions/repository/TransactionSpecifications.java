package ie.universityofgalway.finance.transactions.repository;

import ie.universityofgalway.finance.transactions.entity.Transaction;
import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds reusable query conditions; creating a specification does not execute SQL.
 * Spring Data supplies the entity root and Criteria API objects when applying it.
 * Every filter includes ownership; null optional filters impose no restriction.
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    /**
     * Builds an AND-combined filter for the owner's active transactions.
     *
     * @param userId authenticated owner UUID
     * @param type optional transaction type
     * @param category optional category
     * @param from optional inclusive first calendar day
     * @param to optional inclusive last calendar day
     * @return conditions to apply when the repository executes a query
     */
    public static Specification<Transaction> forHistory(
            UUID userId, TransactionType type, TransactionCategory category, LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = buildFilterPredicates(userId, type, category, from, to, root, criteriaBuilder);

            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Builds an AND-combined filter for the owner's inactive transactions.
     *
     * @param userId authenticated owner UUID
     * @param type optional transaction type
     * @param category optional category
     * @param from optional inclusive first calendar day
     * @param to optional inclusive last calendar day
     * @return conditions to apply when the repository executes a query
     */
    public static Specification<Transaction> forSoftDeleted(
            UUID userId, TransactionType type, TransactionCategory category, LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = buildFilterPredicates(userId, type, category, from, to, root, criteriaBuilder);

            predicates.add(criteriaBuilder.isFalse(root.get("active")));

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Builds conditions, not database results. The lower date boundary is inclusive;
     * the upper boundary is the exclusive start of the following day, including all
     * times on the requested final day without assuming timestamp precision.
     */
    private static List<Predicate> buildFilterPredicates(UUID userId, TransactionType type, TransactionCategory category, LocalDate from, LocalDate to, Root<Transaction> root, CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

        if (type != null) {
            predicates.add(criteriaBuilder.equal(root.get("transactionType"), type));
        }
        if (category != null) {
            predicates.add(criteriaBuilder.equal(root.get("transactionCategory"), category));
        }
        if (from != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("occurredAt"), from.atStartOfDay()));
        }
        if (to != null) {
            predicates.add(criteriaBuilder.lessThan(
                    root.get("occurredAt"), to.plusDays(1).atStartOfDay()));
        }
        return predicates;
    }
}
