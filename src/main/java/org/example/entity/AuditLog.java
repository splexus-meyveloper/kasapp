package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_created_at", columnList = "createdAt"),
                @Index(name = "idx_audit_username", columnList = "username"),
                @Index(name = "idx_audit_action", columnList = "action")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 80)
    private String action; // CASH_INCOME, CASH_EXPENSE, USER_CREATE, ...

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // opsiyonel ama çok faydalı (multi-tenant / firma ayrımı)
    private Long companyId;


}
