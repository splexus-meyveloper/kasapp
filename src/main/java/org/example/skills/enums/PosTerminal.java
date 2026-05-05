package org.example.skills.enums;

public enum PosTerminal {

    // Altikardesler POS terminalleri
    VAKIFBANK        (PosType.ALTIKARDESLER_POS, "Vakifbank"),
    GARANTIBBVA      (PosType.ALTIKARDESLER_POS, "GarantiBBVA"),
    IS_BANKASI       (PosType.ALTIKARDESLER_POS, "Is Bankasi"),
    YAPI_KREDI       (PosType.ALTIKARDESLER_POS, "Yapi Kredi"),
    HALKBANK         (PosType.ALTIKARDESLER_POS, "Halkbank"),
    TEB              (PosType.ALTIKARDESLER_POS, "TEB"),

    // Tedarikci POS terminalleri
    SAMPA            (PosType.TEDARIKCI_POS, "Sampa"),
    HD_KAUCUK        (PosType.TEDARIKCI_POS, "HD Kaucuk"),
    INCITAS          (PosType.TEDARIKCI_POS, "Incitas"),
    MAYSAN           (PosType.TEDARIKCI_POS, "Maysan"),
    MAKPARSAN        (PosType.TEDARIKCI_POS, "Makparsan"),
    ROTA             (PosType.TEDARIKCI_POS, "Rota"),
    OTO_KARAMAN      (PosType.TEDARIKCI_POS, "Oto Karaman"),

    // Yazarkasa POS terminalleri
    YAZARKASA_ZIRAAT (PosType.YAZARKASA_POS, "Ziraat Bankasi"),
    YAZARKASA_TEB    (PosType.YAZARKASA_POS, "TEB");

    private final PosType posType;
    private final String label;

    PosTerminal(PosType posType, String label) {
        this.posType = posType;
        this.label = label;
    }

    public PosType getPosType() { return posType; }
    public String getLabel() { return label; }
}
