package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.skills.enums.PriceStepType;
import org.example.skills.enums.SalesSlot;

import java.math.BigDecimal;

public record PriceRuleStepRequest(

        @NotNull(message = "Adım türü seçilmelidir")
        PriceStepType stepType,

        /** PERCENT_DISCOUNT / PERCENT_MARKUP / MULTIPLY_FACTOR / ADD_FIXED için zorunlu */
        BigDecimal paramNumeric,

        /** APPLY_FX_RATE için para birimi kodu (örn. "USD") */
        String paramText,

        /** ROUND_NEAREST için zorunlu (5/10/50/100 vb.) */
        BigDecimal roundTo,

        /** WRITE_TO_SALES_SLOT için zorunlu */
        SalesSlot targetSlot,

        /** BASE_ON_SALES_SLOT için zorunlu */
        SalesSlot sourceSlot
) {}
