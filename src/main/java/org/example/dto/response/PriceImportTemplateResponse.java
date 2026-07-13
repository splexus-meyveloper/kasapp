package org.example.dto.response;

import org.example.skills.enums.PriceImportTemplateType;

import java.util.Map;

public record PriceImportTemplateResponse(
        Long id,
        PriceImportTemplateType templateType,
        Long supplierId,
        int headerRowIndex,
        Map<String, Integer> fieldMappings
) {}
