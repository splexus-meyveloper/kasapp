package org.example.repository;

import org.example.entity.PriceCodeCrossReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceCodeCrossReferenceRepository extends JpaRepository<PriceCodeCrossReference, Long> {

    List<PriceCodeCrossReference> findByCompanyIdAndSupplierId(Long companyId, Long supplierId);

    Optional<PriceCodeCrossReference> findByCompanyIdAndSupplierIdAndManufacturerCode(
            Long companyId, Long supplierId, String manufacturerCode);
}
