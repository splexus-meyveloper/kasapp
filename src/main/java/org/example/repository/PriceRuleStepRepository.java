package org.example.repository;

import org.example.entity.PriceRuleStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceRuleStepRepository extends JpaRepository<PriceRuleStep, Long> {

    List<PriceRuleStep> findByRuleIdOrderByStepOrderAsc(Long ruleId);

    void deleteByRuleId(Long ruleId);
}
