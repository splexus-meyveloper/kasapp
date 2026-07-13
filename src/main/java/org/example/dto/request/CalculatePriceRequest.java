package org.example.dto.request;

import java.math.BigDecimal;
import java.util.Map;

/** fxRates opsiyoneldir — döviz kuru gerektiren adımlar için ad-hoc kur (Faz B'de kalıcı FxRate devreye girene kadar). */
public record CalculatePriceRequest(
        Map<String, BigDecimal> fxRates
) {}
