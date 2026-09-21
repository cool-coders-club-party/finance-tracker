package ie.universityofgalway.finance.transactions.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Long userId;
    @Enumerated(EnumType.STRING)
    TransactionType transactionType;
    @Enumerated(EnumType.STRING)
    TransactionCategory transactionCategory;
    BigDecimal amount;
    String description;

    @CreationTimestamp
    LocalDateTime createdAt;

}
