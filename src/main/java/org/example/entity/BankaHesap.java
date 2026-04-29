package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_banka_hesap",
        uniqueConstraints = @UniqueConstraint(columnNames = {"hesapKodu", "companyId"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BankaHesap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String hesapKodu;

    @Column(nullable = false)
    private String bankaAdi;

    @Column(nullable = false)
    private String hesapNumarasi;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baslangicBakiye;

    @Column(nullable = false)
    private Long companyId;

    private Long olusturanId;

    private LocalDateTime olusturmaTarihi;

    private boolean aktif = true;
}