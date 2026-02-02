package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_cash_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class CashTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    @Column(nullable = false)
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private Long userId;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private boolean active=true;
}
