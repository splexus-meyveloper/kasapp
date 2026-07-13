package org.example.service;

import org.example.entity.PriceRuleStep;
import org.example.skills.enums.PriceStepType;
import org.example.skills.enums.SalesSlot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriceRuleEngineTest {

    private final PriceRuleEngine engine = new PriceRuleEngine();

    private static PriceRuleStep discount(BigDecimal pct) {
        return PriceRuleStep.builder().stepType(PriceStepType.PERCENT_DISCOUNT).paramNumeric(pct).build();
    }

    private static PriceRuleStep factor(BigDecimal f) {
        return PriceRuleStep.builder().stepType(PriceStepType.MULTIPLY_FACTOR).paramNumeric(f).build();
    }

    private static PriceRuleStep fx(String currency) {
        return PriceRuleStep.builder().stepType(PriceStepType.APPLY_FX_RATE).paramText(currency).build();
    }

    private static PriceRuleStep roundUp() {
        return PriceRuleStep.builder().stepType(PriceStepType.ROUND_UP).build();
    }

    private static PriceRuleStep write(SalesSlot slot) {
        return PriceRuleStep.builder().stepType(PriceStepType.WRITE_TO_SALES_SLOT).targetSlot(slot).build();
    }

    private static PriceRuleStep baseOn(SalesSlot slot) {
        return PriceRuleStep.builder().stepType(PriceStepType.BASE_ON_SALES_SLOT).sourceSlot(slot).build();
    }

    @Test
    void usmerRule_listeIskontoKurKarYuvarla() {
        // %30 iskonto -> USD kuru -> x1.60 -> yukarı yuvarla -> Satış 1
        List<PriceRuleStep> steps = List.of(
                discount(BigDecimal.valueOf(30)),
                fx("USD"),
                factor(BigDecimal.valueOf(1.60)),
                roundUp(),
                write(SalesSlot.SATIS1)
        );

        var ctx = engine.execute(steps, BigDecimal.valueOf(100), Map.of("USD", BigDecimal.valueOf(33.00)));

        assertEquals(0, new BigDecimal("3696.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS1)));
    }

    @Test
    void samparRule_satis1UzerindenSatis3Turetilir() {
        // %30 iskonto -> x1.60 -> yukarı yuvarla = Satış 1 -> Satış1 x1.30 = Satış 3
        List<PriceRuleStep> steps = List.of(
                discount(BigDecimal.valueOf(30)),
                factor(BigDecimal.valueOf(1.60)),
                roundUp(),
                write(SalesSlot.SATIS1),
                baseOn(SalesSlot.SATIS1),
                factor(BigDecimal.valueOf(1.30)),
                write(SalesSlot.SATIS3)
        );

        var ctx = engine.execute(steps, BigDecimal.valueOf(1000), Map.of());

        assertEquals(0, new BigDecimal("1120.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS1)));
        assertEquals(0, new BigDecimal("1456.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS3)));
    }

    @Test
    void maysanMondoRule_ucKademeliZincir() {
        // Net alış x1.40 = Satış4; Satış4 x1.10 + yukarı yuvarla = Satış1; Satış1 x1.05 + yukarı yuvarla = Satış3
        List<PriceRuleStep> steps = List.of(
                factor(BigDecimal.valueOf(1.40)),
                write(SalesSlot.SATIS4),
                factor(BigDecimal.valueOf(1.10)),
                roundUp(),
                write(SalesSlot.SATIS1),
                factor(BigDecimal.valueOf(1.05)),
                roundUp(),
                write(SalesSlot.SATIS3)
        );

        var ctx = engine.execute(steps, BigDecimal.valueOf(500), Map.of());

        assertEquals(0, new BigDecimal("700.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS4)));
        assertEquals(0, new BigDecimal("770.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS1)));
        assertEquals(0, new BigDecimal("809.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS3)));
    }

    @Test
    void roundNearest_enYakinKatinaSepetler() {
        // 773 -> en yakın 50'ye: 773/50=15.46 -> HALF_UP ile 15 -> 15*50=750
        List<PriceRuleStep> steps = List.of(
                PriceRuleStep.builder().stepType(PriceStepType.ROUND_NEAREST).roundTo(BigDecimal.valueOf(50)).build(),
                write(SalesSlot.SATIS1)
        );

        var ctx = engine.execute(steps, BigDecimal.valueOf(773), Map.of());

        assertEquals(0, new BigDecimal("750.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS1)));
    }

    @Test
    void yazilmayanSlotUnutulmusSayilir() {
        // Sadece SATIS1'e yazan bir kuralda SATIS2/3/4 hiç dolmaz — rapor bunları "güncellenmedi" sayabilsin
        List<PriceRuleStep> steps = List.of(factor(BigDecimal.valueOf(2)), write(SalesSlot.SATIS1));

        var ctx = engine.execute(steps, BigDecimal.valueOf(100), Map.of());

        assertEquals(1, ctx.getSalesSlots().size());
        assertEquals(null, ctx.getSalesSlots().get(SalesSlot.SATIS2));
    }

    @Test
    void roundUp_roundToVerilmemisseTamTlYeYuvarlarEskiDavranis() {
        List<PriceRuleStep> steps = List.of(roundUp(), write(SalesSlot.SATIS1));
        var ctx = engine.execute(steps, BigDecimal.valueOf(1141.2), Map.of());
        assertEquals(0, new BigDecimal("1142.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS1)));
    }

    @Test
    void roundUp_kucukYuvarlama_enYakinUsteKatinaYukariYuvarlar() {
        // "Küçük yuvarlama": 1141 -> en yakın 50'nin üstüne -> 1150
        List<PriceRuleStep> steps = List.of(
                PriceRuleStep.builder().stepType(PriceStepType.ROUND_UP).roundTo(BigDecimal.valueOf(50)).build(),
                write(SalesSlot.SATIS1)
        );
        var ctx = engine.execute(steps, BigDecimal.valueOf(1141), Map.of());
        assertEquals(0, new BigDecimal("1150.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS1)));
    }

    @Test
    void roundUp_buyukYuvarlama_enYakinYuzeUsteKatinaYukariYuvarlar() {
        // "Büyük yuvarlama": 1141 -> en yakın 100'ün üstüne -> 1200
        List<PriceRuleStep> steps = List.of(
                PriceRuleStep.builder().stepType(PriceStepType.ROUND_UP).roundTo(BigDecimal.valueOf(100)).build(),
                write(SalesSlot.SATIS1)
        );
        var ctx = engine.execute(steps, BigDecimal.valueOf(1141), Map.of());
        assertEquals(0, new BigDecimal("1200.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS1)));
    }

    @Test
    void roundDown_roundToIleAsagiKatinaYuvarlar() {
        List<PriceRuleStep> steps = List.of(
                PriceRuleStep.builder().stepType(PriceStepType.ROUND_DOWN).roundTo(BigDecimal.valueOf(50)).build(),
                write(SalesSlot.SATIS1)
        );
        var ctx = engine.execute(steps, BigDecimal.valueOf(1141), Map.of());
        assertEquals(0, new BigDecimal("1100.00").compareTo(ctx.getSalesSlots().get(SalesSlot.SATIS1)));
    }

    @Test
    void eksikDovizKuru_hataFirlatir() {
        List<PriceRuleStep> steps = List.of(fx("USD"));
        assertThrows(IllegalStateException.class,
                () -> engine.execute(steps, BigDecimal.valueOf(100), Map.of()));
    }

    @Test
    void yaziliOlmayanSlotaBaseAlma_hataFirlatir() {
        List<PriceRuleStep> steps = List.of(baseOn(SalesSlot.SATIS1));
        assertThrows(IllegalStateException.class,
                () -> engine.execute(steps, BigDecimal.valueOf(100), Map.of()));
    }
}
