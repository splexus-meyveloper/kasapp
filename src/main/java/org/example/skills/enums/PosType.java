package org.example.skills.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PosType {
    ALTIKARDESLER_POS,
    TEDARIKCI_POS,
    YAZARKASA_POS;

    @JsonCreator
    public static PosType from(String value) {
        if (value == null) return null;
        return switch (value.toUpperCase()) {
            case "ALTIKARDESLER_POS", "ALTIKARDESLER" -> ALTIKARDESLER_POS;
            case "TEDARIKCI_POS", "TEDARIKCI" -> TEDARIKCI_POS;
            case "YAZARKASA_POS", "YAZARKASA" -> YAZARKASA_POS;
            default -> throw new IllegalArgumentException("Gecersiz POS tipi: " + value);
        };
    }
}
