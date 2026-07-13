package org.example.dto.response;

import org.example.skills.enums.PriceStepType;
import org.example.skills.enums.SalesSlot;

import java.math.BigDecimal;

public record PriceRuleStepResponse(
        Long id,
        int stepOrder,
        PriceStepType stepType,
        BigDecimal paramNumeric,
        String paramText,
        BigDecimal roundTo,
        SalesSlot targetSlot,
        SalesSlot sourceSlot
) {}
