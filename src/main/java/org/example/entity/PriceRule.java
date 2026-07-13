package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.PriceRuleStatus;

import java.time.LocalDateTime;

/**
 * Fiyat kuralı başlığı — versiyonludur. Aynı ruleGroupKey'i paylaşan satırlar
 * aynı mantıksal kuralın farklı versiyonlarıdır (v1, v2, ...). Bir kural
 * düzenlendiğinde yeni bir versiyon satırı oluşturulur, eskisi ARCHIVED
 * olur — hiçbir satır silinmez veya üzerine yazılmaz.
 */
@Entity
@Table(name = "tbl_price_rule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Column(nullable = false, length = 100)
    private String ruleGroupKey;

    @Column(nullable = false)
    private Integer versionNo;

    @Column(nullable = false, length = 200)
    private String name;

    private Long supplierId;

    /** Null ise bu kural tedarikçinin tüm ürün gruplarına uygulanır. */
    private Long productGroupId;

    @Column(length = 10)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    private PriceRuleStatus status;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long activatedBy;

    private LocalDateTime activatedAt;
}
