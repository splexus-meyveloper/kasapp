package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePriceRuleRequest(

        @NotBlank(message = "Kural adı boş olamaz")
        @Size(max = 200)
        String name,

        @NotNull(message = "Tedarikçi seçilmelidir")
        Long supplierId,

        /** Null ise kural tedarikçinin tüm ürün gruplarına uygulanır. */
        Long productGroupId,

        @Size(max = 10)
        String currencyCode,

        List<PriceRuleStepRequest> steps
) {}
