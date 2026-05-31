package org.example.repository;

import org.example.entity.BankaIslem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BankaIslemRepository extends JpaRepository<BankaIslem, Long> {

    interface BankaIslemRow {
        Long getId();
        String getAciklama();
        BigDecimal getTutar();
        String getIslemKoduRaw();
        String getCustomKodStrRaw();
        String getDirectionRaw();
        String getIslemTarihiRaw();
    }

    Page<BankaIslem> findByHesapIdAndCompanyIdOrderByIslemTarihiDescYuklemeTarihiDesc(
            Long hesapId, Long companyId, Pageable pageable);

    List<BankaIslem> findByHesapIdAndCompanyId(Long hesapId, Long companyId);

    Optional<BankaIslem> findByIdAndHesapIdAndCompanyId(Long id, Long hesapId, Long companyId);

    long countByHesapIdAndCompanyId(Long hesapId, Long companyId);

    void deleteByHesapIdAndCompanyId(Long hesapId, Long companyId);

    @Query(value = """
        SELECT
            i.id AS id,
            i.aciklama AS aciklama,
            i.tutar AS tutar,
            i.islem_kodu AS "islemKoduRaw",
            i.custom_kod_str AS "customKodStrRaw",
            i.direction AS "directionRaw",
            CAST(i.islem_tarihi AS varchar) AS "islemTarihiRaw"
        FROM tbl_banka_islem i
        WHERE i.hesap_id = :hesapId
          AND i.company_id = :companyId
        ORDER BY i.islem_tarihi DESC NULLS LAST, i.yukleme_tarihi DESC NULLS LAST
    """, countQuery = """
        SELECT COUNT(*)
        FROM tbl_banka_islem i
        WHERE i.hesap_id = :hesapId
          AND i.company_id = :companyId
    """, nativeQuery = true)
    Page<BankaIslemRow> findRowsByHesapIdAndCompanyId(
            Long hesapId, Long companyId, Pageable pageable);

    @Query(value = """
        SELECT
            COALESCE(SUM(CASE WHEN i.direction = 'IN' THEN i.tutar ELSE 0 END), 0)
          - COALESCE(SUM(CASE WHEN i.direction = 'OUT' THEN i.tutar ELSE 0 END), 0)
        FROM tbl_banka_islem i
        WHERE i.hesap_id = :hesapId
    """, nativeQuery = true)
    BigDecimal netHareket(Long hesapId);
}
