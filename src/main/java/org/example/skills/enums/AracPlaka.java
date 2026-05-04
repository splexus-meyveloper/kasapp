package org.example.skills.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AracPlaka {
    P_16_AHD_464 ("16 AHD 464"),
    P_16_BVL_436 ("16 BVL 436"),
    P_16_AEA_555 ("16 AEA 555"),
    P_16_ANS_605 ("16 ANS 605"),
    P_16_GD_606  ("16 GD 606"),
    P_16_E_0207  ("16 E 0207"),
    P_16_JUT_88  ("16 JUT 88"),
    P_16_BBR_666 ("16 BBR 666");

    private final String label;
    AracPlaka(String label) { this.label = label; }
    public String getLabel() { return label; }

    /**
     * Frontend bazen enum adini (P_16_AHD_464), bazen de gorunen etiketi (16 AHD 464) gonderebiliyor.
     * Ikisini de kabul edelim ki secim yapildigi halde null'a dusup validasyona takilmasin.
     */
    @JsonCreator
    public static AracPlaka fromJson(String value) {
        if (value == null || value.isBlank()) return null;

        for (AracPlaka p : values()) {
            if (p.name().equalsIgnoreCase(value) || p.label.equalsIgnoreCase(value)) {
                return p;
            }
        }

        throw new IllegalArgumentException("Gecersiz arac plakasi: " + value);
    }
}
