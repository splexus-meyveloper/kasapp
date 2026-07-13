package org.example.dto.response;

import org.example.skills.enums.PriceImportBatchStatus;

public record PriceImportBatchResponse(
        Long id,
        Long supplierId,
        PriceImportBatchStatus status,
        int rowCount
) {}
