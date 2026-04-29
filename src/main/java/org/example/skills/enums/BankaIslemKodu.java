package org.example.skills.enums;

/**
 * Banka işlem kodları - SADECE YAZILIMCI DEĞİŞTİREBİLİR.
 * Hiçbir kullanıcı/admin bu kodlara erişemez.
 *
 * direction: IN  → bakiyeye EKLENIR
 *            OUT → bakiyeden DÜŞÜLÜR
 */
public enum BankaIslemKodu {

    // ── GELİR (IN) ────────────────────────────────────────────────
    GELEN_HAVALE        (100, "Gelen Havale",         Direction.IN),
    GELEN_EFT           (101, "Gelen EFT",             Direction.IN),
    FAIZ_GELIRI         (102, "Faiz Geliri",           Direction.IN),
    MUSTERI_TAHSILATI   (103, "Müşteri Tahsilatı",     Direction.IN),
    KREDI_KULLANIMI     (104, "Kredi Kullanımı",       Direction.IN),
    DIGER_GELIR         (199, "Diğer Gelir",           Direction.IN),

    // ── GİDER (OUT) ───────────────────────────────────────────────
    GIDEN_HAVALE        (200, "Giden Havale",          Direction.OUT),
    GIDEN_EFT           (201, "Giden EFT",             Direction.OUT),
    BANKA_MASRAFI       (770, "Banka Masrafı",         Direction.OUT),
    SAHIS_ODEMESI       (300, "Şahıs Ödemesi",        Direction.OUT),
    TEDARIKCI_ODEMESI   (301, "Tedarikçi Ödemesi",    Direction.OUT),
    VERGI_ODEMESI       (302, "Vergi Ödemesi",         Direction.OUT),
    SGK_ODEMESI         (303, "SGK Ödemesi",           Direction.OUT),
    KIRA_ODEMESI        (304, "Kira Ödemesi",          Direction.OUT),
    KREDI_TAKSITI       (305, "Kredi Taksiti",         Direction.OUT),
    DIGER_GIDER         (399, "Diğer Gider",           Direction.OUT);

    // ─────────────────────────────────────────────────────────────
    public enum Direction { IN, OUT }

    private final int    kod;
    private final String aciklama;
    private final Direction direction;

    BankaIslemKodu(int kod, String aciklama, Direction direction) {
        this.kod       = kod;
        this.aciklama  = aciklama;
        this.direction = direction;
    }

    public int       getKod()       { return kod; }
    public String    getAciklama()  { return aciklama; }
    public Direction getDirection() { return direction; }

    /** Excel'den gelen int kodla eşleştir */
    public static BankaIslemKodu fromKod(int kod) {
        for (BankaIslemKodu b : values()) {
            if (b.kod == kod) return b;
        }
        throw new IllegalArgumentException("Tanımsız işlem kodu: " + kod);
    }
}