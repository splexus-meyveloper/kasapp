package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Üretici fiyat listesinden parse edilmiş tek bir satır. */
@Entity
@Table(name = "tbl_price_import_row")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long batchId;

    @Column(nullable = false, length = 100)
    private String manufacturerCode;

    /** Üretici listesi doğrudan CPM stok kodunu da veriyorsa dolu; genelde null. */
    @Column(length = 100)
    private String stockCode;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal listPrice;

    @Column(length = 10)
    private String currencyCode;
}
