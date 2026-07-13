package org.example.repository;

import org.example.entity.PriceRule;
import org.example.skills.enums.PriceRuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PriceRuleRepository extends JpaRepository<PriceRule, Long> {

    Optional<PriceRule> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
        SELECT r FROM PriceRule r
        WHERE r.companyId = :companyId
          AND (:supplierId IS NULL OR r.supplierId = :supplierId)
          AND (:status IS NULL OR r.status = :status)
        ORDER BY r.name ASC, r.versionNo DESC
    """)
    List<PriceRule> findFiltered(Long companyId, Long supplierId, PriceRuleStatus status);

    List<PriceRule> findByCompanyIdAndRuleGroupKeyOrderByVersionNoDesc(Long companyId, String ruleGroupKey);

    /** Aynı tedarikçi/ürün grubu kombinasyonu için o an aktif kural var mı? (çakışma kontrolü) */
    @Query("""
        SELECT r FROM PriceRule r
        WHERE r.companyId = :companyId
          AND r.supplierId = :supplierId
          AND ((:productGroupId IS NULL AND r.productGroupId IS NULL) OR r.productGroupId = :productGroupId)
          AND r.status = org.example.skills.enums.PriceRuleStatus.ACTIVE
    """)
    Optional<PriceRule> findActiveFor(Long companyId, Long supplierId, Long productGroupId);
}
