package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.BankaIslemKodu;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_banka_islem")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BankaIslem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hesap_id", nullable = false)
    private BankaHesap hesap;

    private String aciklama;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tutar;

    /** Built-in işlem kodu — custom kodlar için null olabilir */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private BankaIslemKodu islemKodu;

    /** Custom işlem kodu string'i — islemKodu null ise bu alan dolu olur */
    @Column(name = "custom_kod_str", length = 50)
    private String customKodStr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankaIslemKodu.Direction direction;

    private LocalDate islemTarihi;

    private Long companyId;

    private Long yuklemeYapanId;

    private LocalDateTime yuklemeTarihi;
}