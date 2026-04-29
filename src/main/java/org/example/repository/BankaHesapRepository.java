package org.example.repository;

import org.example.entity.BankaHesap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankaHesapRepository extends JpaRepository<BankaHesap, Long> {
    List<BankaHesap> findByCompanyIdAndAktifTrue(Long companyId);
    Optional<BankaHesap> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsByHesapKoduAndCompanyId(String hesapKodu, Long companyId);
}