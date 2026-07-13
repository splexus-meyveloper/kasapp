package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fiyat kuralı değişiklik logu — kim, ne zaman, hangi kuralda, hangi alanı
 * neden neye değiştirdi. Merkezi AuditLog/AuditAction sisteminden ayrı
 * tutulur çünkü burada yapılı (alan/eski değer/yeni değer) bir diff
 * gerekiyor; merkezi sistem tutar+açıklama şeklinde daha düz loglar için.
 */
@Entity
@Table(name = "tbl_price_rule_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRuleAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ruleId;

    @Column(nullable = false)
    private Long changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    /** CREATE | UPDATE | VERSION | ACTIVATE */
    @Column(nullable = false, length = 30)
    private String action;

    @Column(length = 100)
    private String fieldChanged;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;
}
