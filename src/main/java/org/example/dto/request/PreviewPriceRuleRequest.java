package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;

public record PreviewPriceRuleRequest(

        @NotNull(message = "Liste fiyatı girilmelidir")
        @Positive
        BigDecimal listPrice,

        /**
         * Opsiyonel — döviz kuru gerektiren adımlar için ad-hoc test kuru
         * (örn. {"USD": 33.00}). Faz B'de kalıcı FxRate servisi devreye
         * girince gerçek kurlar bu parametre olmadan otomatik kullanılacak.
         */
        Map<String, BigDecimal> fxRates
) {}
