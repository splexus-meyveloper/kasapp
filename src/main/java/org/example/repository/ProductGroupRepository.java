package org.example.repository;

import org.example.entity.ProductGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductGroupRepository extends JpaRepository<ProductGroup, Long> {

    List<ProductGroup> findByCompanyIdOrderByNameAsc(Long companyId);

    List<ProductGroup> findByCompanyIdAndSupplierIdOrderByNameAsc(Long companyId, Long supplierId);

    Optional<ProductGroup> findByIdAndCompanyId(Long id, Long companyId);
}
