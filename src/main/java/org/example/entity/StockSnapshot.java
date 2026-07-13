package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** CPM'den çekilen stok listesinden parse edilmiş tek bir satır. */
@Entity
@Table(name = "tbl_price_stock_snapshot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long batchId;

    @Column(nullable = false, length = 100)
    private String stockCode;

    /**
     * CPM'nin "Üretici Mal Kodu" alanı — mal koduyla eşleştirmede birincil yol.
     * Tek hücrede birden fazla kod boşlukla ayrılmış olabilir (örn. "10104    111005"),
     * eşleştirme sırasında bu üç alan da boşluğa göre bölünüp ayrı ayrı denenir.
     */
    @Column(length = 150)
    private String manufacturerCode;

    @Column(length = 150)
    private String manufacturerCode2;

    @Column(length = 150)
    private String manufacturerCode3;

    /** CPM'nin "Araç Türü" alanı (örn. "Ticari", "Binek") — kural çözümlemede ürün grubu olarak kullanılır. */
    @Column(length = 100)
    private String vehicleGroup;

    @Column(length = 100)
    private String barcode;

    @Column(length = 255)
    private String description;

    @Column(precision = 19, scale = 2)
    private BigDecimal currentSales1;

    @Column(precision = 19, scale = 2)
    private BigDecimal currentSales2;

    @Column(precision = 19, scale = 2)
    private BigDecimal currentSales3;

    @Column(precision = 19, scale = 2)
    private BigDecimal currentSales4;
}
