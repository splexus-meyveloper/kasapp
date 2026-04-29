package org.example.skills.enums;

public enum PosTerminal {

    // Altıkardeşler POS terminalleri
    VAKIFBANK        (PosType.ALTIKARDESLER_POS, "Vakıfbank"),
    GARANTIBBVA      (PosType.ALTIKARDESLER_POS, "GarantiBBVA"),
    IS_BANKASI       (PosType.ALTIKARDESLER_POS, "İş Bankası"),
    YAPI_KREDI       (PosType.ALTIKARDESLER_POS, "Yapı Kredi"),
    HALKBANK         (PosType.ALTIKARDESLER_POS, "Halkbank"),
    TEB              (PosType.ALTIKARDESLER_POS, "TEB"),

    // Tedarikçi POS terminalleri
    SAMPA            (PosType.TEDARIKCI_POS, "Sampa"),
    HD_KAUCUK        (PosType.TEDARIKCI_POS, "HD Kauçuk"),
    INCITAS          (PosType.TEDARIKCI_POS, "İncitaş"),
    MAYSAN           (PosType.TEDARIKCI_POS, "Maysan"),
    MAKPARSAN        (PosType.TEDARIKCI_POS, "Makparsan"),
    ROTA             (PosType.TEDARIKCI_POS, "Rota"),
    OTO_KARAMAN      (PosType.TEDARIKCI_POS, "Oto Karaman");

    private final PosType posType;
    private final String  label;

    PosTerminal(PosType posType, String label) {
        this.posType = posType;
        this.label   = label;
    }

    public PosType getPosType() { return posType; }
    public String  getLabel()   { return label; }
}