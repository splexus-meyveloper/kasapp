package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_price_product_group")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    /** Null ise bu grup tüm tedarikçiler için ortak kullanılabilir. */
    private Long supplierId;

    @Column(nullable = false, length = 200)
    private String name;

    @Builder.Default
    private boolean active = true;

    private Long createdBy;

    private LocalDateTime createdAt;
}
