package org.example.dto.response;

import org.example.skills.enums.PriceCalcRunStatus;

public record PriceCalcRunResponse(
        Long id,
        Long batchId,
        PriceCalcRunStatus status,
        int totalMatched,
        int totalUnmatched
) {}
