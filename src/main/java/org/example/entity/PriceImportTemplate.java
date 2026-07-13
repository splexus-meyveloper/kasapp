package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.PriceImportTemplateType;

/**
 * Kolon eşleme şablonu — Excel'in gerçek kolon yapısı değişse bile (yeni
 * üretici, CPM formatı güncellemesi) kod değişmeden burada güncellenir.
 * fieldMappingsJson: {"MANUFACTURER_CODE": 2, "LIST_PRICE": 6, ...}
 * — alan adı -> 0 tabanlı kolon indeksi. MANUFACTURER_LIST şablonları
 * supplierId'ye bağlıdır (her tedarikçinin listesi farklı), CPM_STOCK ve
 * CPM_EXPORT şirket geneli tek şablondur (supplierId null).
 */
@Entity
@Table(name = "tbl_price_import_template")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceImportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PriceImportTemplateType templateType;

    /** MANUFACTURER_LIST için zorunlu; CPM_STOCK/CPM_EXPORT için null (şirket geneli). */
    private Long supplierId;

    @Builder.Default
    private int headerRowIndex = 0;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String fieldMappingsJson;
}
