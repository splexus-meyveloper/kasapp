package org.example.repository;

import org.example.entity.PriceImportTemplate;
import org.example.skills.enums.PriceImportTemplateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceImportTemplateRepository extends JpaRepository<PriceImportTemplate, Long> {

    List<PriceImportTemplate> findByCompanyId(Long companyId);

    Optional<PriceImportTemplate> findByCompanyIdAndTemplateTypeAndSupplierId(
            Long companyId, PriceImportTemplateType templateType, Long supplierId);

    /** CPM_STOCK / CPM_EXPORT gibi şirket geneli (supplierId=null) şablonlar için. */
    Optional<PriceImportTemplate> findByCompanyIdAndTemplateTypeAndSupplierIdIsNull(
            Long companyId, PriceImportTemplateType templateType);
}
