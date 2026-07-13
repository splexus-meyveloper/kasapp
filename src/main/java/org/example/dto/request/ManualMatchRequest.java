package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record ManualMatchRequest(
        @NotNull(message = "Üretici listesi satırı seçilmelidir")
        Long manufacturerRowId,

        /** Kuralda döviz kuru adımı varsa — hesaplama ekranında girilen ad-hoc kurlar. */
        Map<String, BigDecimal> fxRates
) {}
