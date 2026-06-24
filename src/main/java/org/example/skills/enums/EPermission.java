package org.example.skills.enums;

public enum EPermission {
    KASA,
    CEK,
    SENET,
    MASRAF,
    KREDILER,
    KULLANICI_YONETIMI,
    BANKA,
    /** Bu yetkiye sahip kullanıcılar Sil/Düzenle işlemlerini admin onayı beklemeden anında uygular. */
    DOGRUDAN_ISLEM,
    /** Bu yetkiye sahip kullanıcılar geçmiş tarihli işlem girebilir. */
    GECMIS_TARIH
}