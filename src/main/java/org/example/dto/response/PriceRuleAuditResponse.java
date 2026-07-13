package org.example.dto.response;

import java.time.LocalDateTime;

public record PriceRuleAuditResponse(
        Long id,
        Long changedBy,
        String changedByUsername,
        LocalDateTime changedAt,
        String action,
        String fieldChanged,
        String oldValue,
        String newValue
) {}
