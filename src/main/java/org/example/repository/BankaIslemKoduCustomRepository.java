package org.example.repository;

import org.example.entity.BankaIslemKoduCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankaIslemKoduCustomRepository extends JpaRepository<BankaIslemKoduCustom, Long> {

    List<BankaIslemKoduCustom> findByCompanyId(Long companyId);

    boolean existsByKodAndCompanyId(String kod, Long companyId);

    Optional<BankaIslemKoduCustom> findByIdAndCompanyId(Long id, Long companyId);
}
