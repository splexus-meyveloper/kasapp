package org.example.dto.response;

import java.math.BigDecimal;

public record PriceCalcResultResponse(
        Long id,
        String stockCode,
        String manufacturerCode,
        String productName,
        boolean matched,
        Long ruleId,
        Integer ruleVersionNo,
        BigDecimal netAlis,
        BigDecimal oldSales1, BigDecimal oldSales2, BigDecimal oldSales3, BigDecimal oldSales4,
        BigDecimal newSales1, BigDecimal newSales2, BigDecimal newSales3, BigDecimal newSales4,
        BigDecimal changePercent,
        String updatedSlots,
        String reason
) {}
