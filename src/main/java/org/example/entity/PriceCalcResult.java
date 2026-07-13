package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Bir hesaplama çalıştırmasının satır bazlı sonucu — hem rapor hem
 * CPM export'unun kaynağıdır. netAlis, kural motoruna girdi olarak
 * verilen fiyattır (CPM export'ta "Alış 1" kolonuna yazılır).
 */
@Entity
@Table(name = "tbl_price_calc_result")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalcResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long calcRunId;

    private String stockCode;

    private String manufacturerCode;

    @Column(length = 255)
    private String productName;

    private boolean matched;

    private Long ruleId;

    private Integer ruleVersionNo;

    @Column(precision = 19, scale = 4)
    private BigDecimal netAlis;

    @Column(precision = 19, scale = 2)
    private BigDecimal oldSales1;
    @Column(precision = 19, scale = 2)
    private BigDecimal oldSales2;
    @Column(precision = 19, scale = 2)
    private BigDecimal oldSales3;
    @Column(precision = 19, scale = 2)
    private BigDecimal oldSales4;

    @Column(precision = 19, scale = 2)
    private BigDecimal newSales1;
    @Column(precision = 19, scale = 2)
    private BigDecimal newSales2;
    @Column(precision = 19, scale = 2)
    private BigDecimal newSales3;
    @Column(precision = 19, scale = 2)
    private BigDecimal newSales4;

    @Column(precision = 7, scale = 2)
    private BigDecimal changePercent;

    /** Hangi slotlar güncellendi — örn. "SATIS1,SATIS3" */
    @Column(length = 50)
    private String updatedSlots;

    /** Eşleşmedi veya kural bulunamadıysa neden — rapor için */
    @Column(length = 255)
    private String reason;
}
