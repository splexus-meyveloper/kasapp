package org.example.skills.enums;

public enum NoteStatus {

    PORTFOYDE,
    TAHSIL_EDILDI,
    TEMINATA_CIKTI,
    CIRO_EDILDI,

    // Sorunlu senet — giriş
    PROTESTOLU,

    // Sorunlu senetten çıkış
    MUSTERI_IADE,
    AVUKATA_CIKIS,

    // Tahsil/cirodan portföye geri dönüş
    IADE_EDILDI
}
