package org.example.service;

import org.example.entity.PriceRuleStep;
import org.example.skills.enums.SalesSlot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fiyat kural motorunun hesaplama çekirdeği. Bir kuralın adımlarını sırayla
 * bir CalcContext üzerinde uygular; her adım saf bir dönüşümdür, sırası
 * değiştirilemez (stepOrder ile gelir). Bir satış slotu yalnızca
 * WRITE_TO_SALES_SLOT adımıyla açıkça yazılırsa dolar — hangi slotların
 * güncellendiğini raporlamak bu yüzden mümkün (dokunulmayanlar boş kalır).
 */
@Component
public class PriceRuleEngine {

    private static final int CALC_SCALE = 6;

    public static class CalcContext {
        private BigDecimal currentValue;
        private final BigDecimal listPrice;
        private final Map<SalesSlot, BigDecimal> salesSlots = new EnumMap<>(SalesSlot.class);
        private final Map<String, BigDecimal> fxRates;

        public CalcContext(BigDecimal listPrice, Map<String, BigDecimal> fxRates) {
            this.listPrice = listPrice;
            this.currentValue = listPrice;
            this.fxRates = fxRates != null ? fxRates : Map.of();
        }

        public BigDecimal getCurrentValue() { return currentValue; }
        public BigDecimal getListPrice() { return listPrice; }
        public Map<SalesSlot, BigDecimal> getSalesSlots() { return salesSlots; }
    }

    /**
     * @param steps    stepOrder'a göre sıralı adımlar (çağıran taraf sıralı vermeli)
     * @param listPrice hesaplamanın başlangıç değeri
     * @param fxRates  APPLY_FX_RATE adımları için para birimi kodu -> kur haritası
     */
    public CalcContext execute(List<PriceRuleStep> steps, BigDecimal listPrice, Map<String, BigDecimal> fxRates) {
        CalcContext ctx = new CalcContext(listPrice, fxRates);
        for (PriceRuleStep step : steps) {
            applyStep(ctx, step);
        }
        return ctx;
    }

    private void applyStep(CalcContext ctx, PriceRuleStep step) {
        switch (step.getStepType()) {
            case PERCENT_DISCOUNT -> {
                BigDecimal factor = BigDecimal.ONE.subtract(
                        step.getParamNumeric().divide(BigDecimal.valueOf(100), CALC_SCALE, RoundingMode.HALF_UP));
                ctx.currentValue = ctx.currentValue.multiply(factor).setScale(CALC_SCALE, RoundingMode.HALF_UP);
            }
            case PERCENT_MARKUP -> {
                BigDecimal factor = BigDecimal.ONE.add(
                        step.getParamNumeric().divide(BigDecimal.valueOf(100), CALC_SCALE, RoundingMode.HALF_UP));
                ctx.currentValue = ctx.currentValue.multiply(factor).setScale(CALC_SCALE, RoundingMode.HALF_UP);
            }
            case MULTIPLY_FACTOR ->
                ctx.currentValue = ctx.currentValue.multiply(step.getParamNumeric()).setScale(CALC_SCALE, RoundingMode.HALF_UP);
            case ADD_FIXED ->
                ctx.currentValue = ctx.currentValue.add(step.getParamNumeric()).setScale(CALC_SCALE, RoundingMode.HALF_UP);
            case APPLY_FX_RATE -> {
                BigDecimal rate = ctx.fxRates.get(step.getParamText());
                if (rate == null) {
                    throw new IllegalStateException("Döviz kuru bulunamadı: " + step.getParamText());
                }
                ctx.currentValue = ctx.currentValue.multiply(rate).setScale(CALC_SCALE, RoundingMode.HALF_UP);
            }
            case ROUND_UP -> {
                // roundTo verilmemişse eski davranış korunur (tam TL'ye yukarı yuvarla).
                // Verilmişse "küçük/büyük yuvarlama" için istenen katına yukarı yuvarlar
                // (örn. roundTo=50 -> 1141 olur 1150, roundTo=100 -> 1141 olur 1200).
                BigDecimal bucket = step.getRoundTo() != null ? step.getRoundTo() : BigDecimal.ONE;
                ctx.currentValue = ctx.currentValue.divide(bucket, 0, RoundingMode.CEILING).multiply(bucket);
            }
            case ROUND_DOWN -> {
                BigDecimal bucket = step.getRoundTo() != null ? step.getRoundTo() : BigDecimal.ONE;
                ctx.currentValue = ctx.currentValue.divide(bucket, 0, RoundingMode.FLOOR).multiply(bucket);
            }
            case ROUND_NEAREST -> {
                BigDecimal bucket = step.getRoundTo();
                ctx.currentValue = ctx.currentValue
                        .divide(bucket, 0, RoundingMode.HALF_UP)
                        .multiply(bucket);
            }
            case WRITE_TO_SALES_SLOT ->
                ctx.salesSlots.put(step.getTargetSlot(), ctx.currentValue.setScale(2, RoundingMode.HALF_UP));
            case BASE_ON_SALES_SLOT -> {
                BigDecimal base = ctx.salesSlots.get(step.getSourceSlot());
                if (base == null) {
                    throw new IllegalStateException(
                            "Referans verilen satış slotu henüz yazılmamış: " + step.getSourceSlot());
                }
                ctx.currentValue = base;
            }
        }
    }
}
