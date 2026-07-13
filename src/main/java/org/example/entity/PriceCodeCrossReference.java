package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Kalıcı eşleştirme hafızası — bir üretici kodu bir kez (otomatik ya da elle)
 * bir CPM stok koduna eşleştirilince burada saklanır. Bir sonraki fiyat
 * güncellemesinde (2-3 ay sonra) bu tablo önce kontrol edilir; CPM'in
 * "Üretici Mal Kodu" alanı o çalıştırmada boş/hatalı olsa bile ürün
 * bir daha "eşleşmedi"ye düşmez.
 */
@Entity
@Table(name = "tbl_price_code_xref", uniqueConstraints = @UniqueConstraint(columnNames = {"companyId", "supplierId", "manufacturerCode"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCodeCrossReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false, length = 150)
    private String manufacturerCode;

    @Column(nullable = false, length = 100)
    private String stockCode;

    /** AUTO (Üretici Mal Kodu alanından otomatik) | MANUAL (kullanıcı elle eşleştirdi) */
    @Column(nullable = false, length = 10)
    private String source;

    private Long createdBy;

    private LocalDateTime createdAt;
}
