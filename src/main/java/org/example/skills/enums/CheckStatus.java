package org.example.skills.enums;

public enum CheckStatus {

    // Portföyde bekliyor
    PORTFOYDE,

    // Normal çıkış yolları
    TAHSIL_EDILDI,
    TEMINATA_CIKTI,
    CIRO_EDILDI,
    ODENDI,

    // Sorunlu çekler — giriş yöntemi
    KARSILISIZ,
    PROTESTOLU,

    // Sorunlu çeklerden çıkış yolları
    MUSTERI_IADE,
    AVUKATA_CIKIS,

    // Tahsil/ciro'dan iade — portföye geri döndü
    IADE_EDILDI
}
