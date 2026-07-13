package org.example.skills.enums;

/**
 * Fiyat kural motorunun formül adımı türleri. Her tür CalcContext üzerinde
 * sırayla uygulanır — bkz. org.example.service.PriceRuleEngine.
 */
public enum PriceStepType {
    /** currentValue *= (1 - paramNumeric/100) */
    PERCENT_DISCOUNT,
    /** currentValue *= (1 + paramNumeric/100) */
    PERCENT_MARKUP,
    /** currentValue *= paramNumeric */
    MULTIPLY_FACTOR,
    /** currentValue += paramNumeric */
    ADD_FIXED,
    /** currentValue *= fxRates[paramText] */
    APPLY_FX_RATE,
    /** currentValue = ceil(currentValue) — tam TL'ye yukarı yuvarlar */
    ROUND_UP,
    /** currentValue = floor(currentValue) — tam TL'ye aşağı yuvarlar */
    ROUND_DOWN,
    /** currentValue en yakın roundTo katına (5/10/50/100) yuvarlanır */
    ROUND_NEAREST,
    /** salesSlots[targetSlot] = currentValue — currentValue değişmez, zincir devam edebilir */
    WRITE_TO_SALES_SLOT,
    /** currentValue = salesSlots[sourceSlot] — yeni bir hesap dalı başlatır */
    BASE_ON_SALES_SLOT
}
