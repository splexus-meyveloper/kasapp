package org.example.dto.response;

import org.example.skills.enums.PriceRuleStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PriceRuleResponse(
        Long id,
        String ruleGroupKey,
        int versionNo,
        String name,
        Long supplierId,
        Long productGroupId,
        String currencyCode,
        PriceRuleStatus status,
        List<PriceRuleStepResponse> steps,
        LocalDateTime createdAt,
        LocalDateTime activatedAt
) {}
