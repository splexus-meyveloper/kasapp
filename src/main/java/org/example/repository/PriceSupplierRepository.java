package org.example.repository;

import org.example.entity.PriceSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceSupplierRepository extends JpaRepository<PriceSupplier, Long> {

    List<PriceSupplier> findByCompanyIdOrderByNameAsc(Long companyId);

    Optional<PriceSupplier> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCodeAndCompanyId(String code, Long companyId);
}
