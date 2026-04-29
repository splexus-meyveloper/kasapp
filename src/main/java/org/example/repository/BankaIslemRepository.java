package org.example.repository;

import org.example.entity.BankaIslem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface BankaIslemRepository extends JpaRepository<BankaIslem, Long> {

    Page<BankaIslem> findByHesapIdAndCompanyIdOrderByIslemTarihiDescYuklemeTarihiDesc(
            Long hesapId, Long companyId, Pageable pageable);

    List<BankaIslem> findByHesapIdAndCompanyId(Long hesapId, Long companyId);

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN i.direction = 'IN'  THEN i.tutar ELSE 0 END), 0)
             - COALESCE(SUM(CASE WHEN i.direction = 'OUT' THEN i.tutar ELSE 0 END), 0)
        FROM BankaIslem i WHERE i.hesap.id = :hesapId
    """)
    BigDecimal netHareket(Long hesapId);
}