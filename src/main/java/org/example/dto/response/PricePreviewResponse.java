package org.example.dto.response;

import org.example.skills.enums.SalesSlot;

import java.math.BigDecimal;
import java.util.Map;

public record PricePreviewResponse(
        BigDecimal listPrice,
        Map<SalesSlot, BigDecimal> salesSlots,
        String error
) {}
