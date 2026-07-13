package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.PriceStepType;
import org.example.skills.enums.SalesSlot;

import java.math.BigDecimal;

/**
 * Bir PriceRule versiyonuna ait tek bir formül adımı. Bir kural versiyonu
 * yayınlandıktan sonra adımları immutable kabul edilir — düzenleme,
 * yeni versiyon oluşturup adımları kopyalayarak yapılır.
 */
@Entity
@Table(name = "tbl_price_rule_step")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRuleStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ruleId;

    @Column(nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceStepType stepType;

    @Column(precision = 19, scale = 6)
    private BigDecimal paramNumeric;

    @Column(length = 20)
    private String paramText;

    @Column(precision = 19, scale = 2)
    private BigDecimal roundTo;

    @Enumerated(EnumType.STRING)
    private SalesSlot targetSlot;

    @Enumerated(EnumType.STRING)
    private SalesSlot sourceSlot;
}
