package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_price_supplier", uniqueConstraints = @UniqueConstraint(columnNames = {"code", "companyId"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Builder.Default
    private boolean active = true;

    /**
     * Üretici kodunda geçen ama CPM'in "Üretici Mal Kodu" alanında olmayan
     * bilinen ön ekler (virgülle ayrılmış, örn. "US,USA"). Doğrudan eşleşme
     * tutmazsa bu ön ekler koddan temizlenip tekrar denenir. Kullanıcı bir
     * kez tanımlar, sonraki tüm çalıştırmalarda otomatik uygulanır.
     */
    @Column(length = 300)
    private String ignoredCodePrefixes;

    private Long createdBy;

    private LocalDateTime createdAt;
}
