package org.example.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.example.skills.enums.PriceImportTemplateType;

import java.util.Map;

public record SavePriceImportTemplateRequest(

        @NotNull(message = "Şablon türü seçilmelidir")
        PriceImportTemplateType templateType,

        /** MANUFACTURER_LIST için zorunlu; CPM_STOCK/CPM_EXPORT için null bırakın. */
        Long supplierId,

        int headerRowIndex,

        @NotEmpty(message = "En az bir kolon eşlemesi girilmelidir")
        Map<String, Integer> fieldMappings
) {}
