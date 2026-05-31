package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_banka_islem_kodu_custom")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankaIslemKoduCustom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Örn: "309.99.001" */
    @Column(nullable = false, length = 50)
    private String kod;

    @Column(length = 255)
    private String aciklama;

    /** "IN" veya "OUT" */
    @Column(nullable = false, length = 3)
    private String direction;

    @Column(nullable = false)
    private Long companyId;

    private LocalDateTime createdAt;
}
