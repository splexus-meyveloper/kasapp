package org.example.repository;

import org.example.entity.PriceRuleAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceRuleAuditRepository extends JpaRepository<PriceRuleAudit, Long> {

    List<PriceRuleAudit> findByRuleIdOrderByChangedAtDesc(Long ruleId);

    void deleteByRuleId(Long ruleId);
}
