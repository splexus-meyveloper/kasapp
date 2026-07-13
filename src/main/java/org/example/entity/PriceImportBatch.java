package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.PriceImportBatchStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_price_import_batch")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Column(nullable = false)
    private Long supplierId;

    /** Üretici, ürünlerini gruba göre (ör. Ticari/Binek) ayrı Excel dosyalarında gönderiyorsa,
     * bu batch'in ait olduğu grup — kural çözümlemede kullanılır. Grup ayrımı yoksa null. */
    private Long productGroupId;

    private String manufacturerFileName;

    private String stockFileName;

    private Long uploadedBy;

    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PriceImportBatchStatus status;
}
