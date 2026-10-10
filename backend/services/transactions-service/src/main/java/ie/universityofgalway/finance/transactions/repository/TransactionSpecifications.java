package ie.universityofgalway.finance.transactions.repository;

import ie.universityofgalway.finance.transactions.entity.Transaction;
import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> forHistory(
            Long userId, TransactionType type, TransactionCategory category, LocalDate from, LocalDate to) {
        // root = transaction row
        return (root, query, criteriaBuilder) -> {
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

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
