package org.example.skills.enums;

public enum AuditAction {

    CASH_INCOME,
    CASH_EXPENSE,

    CHECK_IN,
    CHECK_COLLECT,
    CHECK_ENDORSE,
    CHECK_OUT,
    CHECK_PAID,

    NOTE_IN,
    NOTE_COLLECT,
    NOTE_ENDORSE,
    NOTE_OUT,

    LOAN_CREATE,
    LOAN_INSTALLMENT,

    CASH_UPDATE_REQUEST_CREATED,
    CASH_UPDATE_REQUEST_APPROVED,
    CASH_UPDATE_REQUEST_REJECTED,

    CHECK_UPDATE_REQUEST_CREATED,
    CHECK_UPDATE_REQUEST_APPROVED,
    CHECK_UPDATE_REQUEST_REJECTED,

    NOTE_UPDATE_REQUEST_CREATED,
    NOTE_UPDATE_REQUEST_APPROVED,
    NOTE_UPDATE_REQUEST_REJECTED,

    POS_UPDATE_REQUEST_CREATED,
    POS_UPDATE_REQUEST_APPROVED,
    POS_UPDATE_REQUEST_REJECTED,

    EXPENSE_UPDATE_REQUEST_CREATED,
    EXPENSE_UPDATE_REQUEST_APPROVED,
    EXPENSE_UPDATE_REQUEST_REJECTED,

    // İşlem silme (onaylı)
    ISLEM_SILME_TALEBI,         // silme talebi oluşturuldu
    ISLEM_SILINDI,              // silme onaylandı → işlem silindi + finansal etki geri alındı
    ISLEM_SILME_REDDEDILDI,     // silme reddedildi

    EXPENSE_ADD,

    POS_LOG,                    // Kredi kartı POS kaydı
    BANKA_CIKIS,                // Kasadan bankaya para çıkışı (sadece admin)

    // Çek/senet iade (tahsil veya cirodan portföye geri dönüş)
    CHECK_IADE,
    NOTE_IADE,

    // Karşılıksız / protestolu giriş
    CHECK_KARSILISIZ,
    CHECK_PROTESTOLU,
    NOTE_KARSILISIZ,
    NOTE_PROTESTOLU,

    // Sorunlu çekten çıkış yolları
    CHECK_MUSTERI_IADE,
    CHECK_AVUKAT_CIKIS,
    NOTE_MUSTERI_IADE,
    NOTE_AVUKAT_CIKIS,

    // Şubeler arası transfer
    INTER_BRANCH_TRANSFER_CREATED,
    INTER_BRANCH_TRANSFER_APPROVED,
    INTER_BRANCH_TRANSFER_REJECTED
}
