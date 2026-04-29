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
    OLAGAN_GELIR_KARLAR         ("679.03.001", "Olağan Gelir ve Karlar",                    Direction.IN),

    // Tahsile Verilen Çekler
    TAHSIL_CEK_HALK             ("101.01.001", "Tahsile Verilen Çekler (Halk)",             Direction.IN),
    TAHSIL_CEK_GARANTI          ("101.02.001", "Tahsile Verilen Çekler (Garanti)",          Direction.IN),
    TAHSIL_CEK_ISBANK           ("101.03.001", "Tahsile Verilen Çekler (İşbank)",           Direction.IN),
    TAHSIL_CEK_TEB              ("101.04.001", "Tahsile Verilen Çekler (TEB)",              Direction.IN),
    TAHSIL_CEK_ZIRAAT           ("101.06.001", "Tahsile Verilen Çekler (Ziraat)",           Direction.IN),
    TAHSIL_CEK_VAKIF            ("101.08.001", "Tahsile Verilen Çekler (Vakıf)",            Direction.IN),
    TAHSIL_CEK_ZIRAAT_KATILIM   ("101.09.001", "Tahsile Verilen Çekler (Ziraat Katılım)",  Direction.IN),
    TAHSIL_CEK_KUVEYTTURK       ("101.11.001", "Tahsile Verilen Çekler (KuveytTürk)",      Direction.IN),

    // Kredi Kartı ile Satışlar
    KK_HALK                     ("108.01.001", "Halk Bankası Kredi Kartı Satış",            Direction.IN),
    KK_GARANTI                  ("108.02.001", "Garanti Bankası Kredi Kartı Satış",         Direction.IN),
    KK_ISBANK                   ("108.03.001", "İş Bankası Kredi Kartı Satış",              Direction.IN),
    KK_TEB                      ("108.04.001", "TEB Kredi Kartı Satış",                     Direction.IN),
    KK_YAPI_KREDI               ("108.05.001", "Yapı Kredi Kredi Kartı Satış",              Direction.IN),
    KK_BURSA_YAZARKASA          ("108.06.001", "Bursa Yazarkasa Kredi Kartı",               Direction.IN),
    KK_ZIRAAT_KATILIM           ("108.09.001", "Ziraat Katılım Kredi Kartı",                Direction.IN),
    KK_VAKIFBANK                ("108.13.001", "Vakıfbank Kredi Kartı Satış",               Direction.IN),

    // Tahsile Verilen Senetler
    TAHSIL_SENET_HALK           ("121.02.001", "Tahsile Verilen Senet (Halk)",              Direction.IN),
    TAHSIL_SENET_VAKIF          ("121.08.001", "Tahsile Verilen Senet (Vakıf)",             Direction.IN),

    // Kredi Kullanımları
    KREDI_HALK_ROTATIF          ("300.01.015", "Halk Bankası Rotatif Kredi",                Direction.IN),
    KREDI_HALK_LR002175         ("300.01.021", "Halk Bankası LR002175 Kredi",               Direction.IN),
    KREDI_GARANTI_8204146       ("300.02.007", "Garanti 8204146 Kredi",                     Direction.IN),
    KREDI_GARANTI_8209999       ("300.02.008", "Garanti 8209999 Kredi",                     Direction.IN),
    KREDI_ISBANK_2212           ("300.03.009", "İş Bankası 2212 Kredi",                     Direction.IN),
    KREDI_ISBANK_1152           ("300.03.010", "İş Bankası 1152 KMH",                       Direction.IN),
    KREDI_ISBANK_99             ("300.03.011", "İş Bankası 99 KMH",                         Direction.IN),
    KREDI_ISBANK_1640           ("300.03.012", "İş Bankası 1640 MKH",                       Direction.IN),
    KREDI_TEB_127802820         ("300.04.009", "TEB 127802820 Kredi",                       Direction.IN),
    KREDI_TEB_138171949         ("300.04.016", "TEB 138171949 Kredi",                       Direction.IN),
    KREDI_TEB_ROTATIF           ("300.04.017", "TEB Rotatif Kredi",                         Direction.IN),
    KREDI_TEB_150125686         ("300.04.018", "TEB 150125686 Kredi",                       Direction.IN),
    KREDI_YAPI_81048292         ("300.05.001", "Yapı Kredi 81048292 Kredi",                 Direction.IN),
    KREDI_YAPI_244456486282     ("300.05.005", "Yapı Kredi 244456486282 Kredi",             Direction.IN),
    KREDI_YAPI_251506900861     ("300.05.013", "Yapı Kredi 251506900861 Kredi",             Direction.IN),
    KREDI_YAPI_252312988681     ("300.05.014", "Yapı Kredi 252312988681 Kredi",             Direction.IN),
    KREDI_ZIRAAT_EYT_1037       ("300.06.008", "Ziraat EYT 1037 Kredi",                    Direction.IN),
    KREDI_ZIRAAT_1017           ("300.06.010", "Ziraat 1017 KMH",                           Direction.IN),
    KREDI_ZIRAAT_ROTATIF        ("300.06.011", "Ziraat Rotatif Kredi",                      Direction.IN),
    KREDI_ZIRAAT_1049           ("300.06.012", "Ziraat 1049 Kredi",                         Direction.IN),
    KREDI_ZIRAAT_1055           ("300.06.013", "Ziraat 1055 Kredi",                         Direction.IN),
    KREDI_ZIRAAT_1052           ("300.06.014", "Ziraat 1052 Kredi",                         Direction.IN),
    KREDI_ZIRAAT_1064           ("300.06.018", "Ziraat 1064 Rotatif Kredi",                 Direction.IN),
    KREDI_VAKIF_6501243585      ("300.08.002", "Vakıfbank 6501243585 Tam Çıpa",             Direction.IN),
    KREDI_VAKIF_BCH             ("300.08.003", "Vakıfbank BCH Kredi",                       Direction.IN),
    KREDI_VAKIF_6501460747      ("300.08.004", "Vakıfbank 6501460747 Kredi",                Direction.IN),
    KREDI_ZK_57                 ("300.09.013", "Ziraat Katılım 1.500.000 (57)",             Direction.IN),
    KREDI_ZK_70                 ("300.09.014", "Ziraat Katılım 500.000 (70)",               Direction.IN),
    KREDI_ZK_73                 ("300.09.015", "Ziraat Katılım 600.000 (73)",               Direction.IN),
    KREDI_ZK_76                 ("300.09.016", "Ziraat Katılım 1.150.000 (76)",             Direction.IN),
    KREDI_ZK_79                 ("300.09.017", "Ziraat Katılım 2.230.000 (79)",             Direction.IN),
    KREDI_ZK_82                 ("300.09.018", "Ziraat Katılım 525.000 (82)",               Direction.IN),
    KREDI_ZK_85                 ("300.09.019", "Ziraat Katılım 1.500.000 (85)",             Direction.IN),
    KREDI_ZK_88                 ("300.09.020", "Ziraat Katılım 5.000.000 (88)",             Direction.IN),
    KREDI_ZK_92                 ("300.09.021", "Ziraat Katılım 900.000 (92)",               Direction.IN),
    KREDI_ZK_95                 ("300.09.022", "Ziraat Katılım 300.000 (95)",               Direction.IN),
    KREDI_ZK_98                 ("300.09.023", "Ziraat Katılım 1.400.000 (98)",             Direction.IN),

    // ── GİDER (OUT) ───────────────────────────────────────────────
    BANKA_MASRAFLARI            ("770.01.010", "Banka Masrafları",                          Direction.OUT),
    VERGI_VE_HARCLAR            ("770.01.019", "Vergi ve Harçlar",                          Direction.OUT),
    FINANSMAN_GIDERLERI         ("780.01.001", "Finansman Giderleri",                       Direction.OUT),
    PAZARLAMA_TASIT_GIDERI      ("760.01.004", "Pazarlama Taşıt Gideri",                   Direction.OUT),
    OLAGAN_GIDER_ZARARLAR       ("689.01.003", "Olağan Gider ve Zararlar",                  Direction.OUT),
    IPEK_TIK                    ("336.01.006", "İpek Tik",                                  Direction.OUT),
    MEHMET_RUHI_OZONAR          ("336.01.007", "Mehmet Ruhi Özonar (10/A Blok Depo)",       Direction.OUT),
    NILUFER_BELEDIYESI_KIRA     ("329.01.007", "Nilüfer Belediyesi (Kira)",                 Direction.OUT),
    ATINC_ALTIKARDESLER         ("131.02.001", "M.Atınç Altıkardeşler",                     Direction.OUT),
    KIVANC_ALTIKARDESLER        ("131.03.001", "Kıvanç Altıkardeşler",                      Direction.OUT);

    // ─────────────────────────────────────────────────────────────
    public enum Direction { IN, OUT }

    private final String   kod;
    private final String   aciklama;
    private final Direction direction;

    BankaIslemKodu(String kod, String aciklama, Direction direction) {
        this.kod       = kod;
        this.aciklama  = aciklama;
        this.direction = direction;
    }

    public String    getKod()       { return kod; }
    public String    getAciklama()  { return aciklama; }
    public Direction getDirection() { return direction; }

    /** Excel'den gelen string kodla eşleştir */
    public static BankaIslemKodu fromKod(String kod) {
        if (kod == null) return null;
        String temiz = kod.trim();
        for (BankaIslemKodu b : values()) {
            if (b.kod.equals(temiz)) return b;
        }
        return null; // tanımsız kod → satır atlanır
    }
}