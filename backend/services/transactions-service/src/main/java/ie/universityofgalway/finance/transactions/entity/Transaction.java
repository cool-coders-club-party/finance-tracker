package ie.universityofgalway.finance.transactions.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;
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
    LocalDateTime occurredAt;

    @CreationTimestamp
    LocalDateTime createdAt;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "active", nullable = false)
    Boolean active = true;

}
