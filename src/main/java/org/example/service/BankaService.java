package org.example.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dto.request.BankaHesapOlusturRequest;
import org.example.dto.response.BankaHesapResponse;
import org.example.dto.response.BankaIslemKoduResponse;
import org.example.dto.response.BankaIslemResponse;
import org.example.dto.response.BankaKategoriOzet;
import org.example.entity.BankaHesap;
import org.example.entity.BankaIslem;
import org.example.entity.BankaIslemKoduCustom;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.BankaHesapRepository;
import org.example.repository.BankaIslemKoduCustomRepository;
import org.example.repository.BankaIslemRepository;
import org.example.skills.enums.BankaIslemKodu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BankaService {

    private static final Logger log = LoggerFactory.getLogger(BankaService.class);

    private final BankaHesapRepository hesapRepo;
    private final BankaIslemRepository islemRepo;
    private final BankaIslemKoduCustomRepository customKodRepo;

    @Transactional
    public BankaHesapResponse hesapOlustur(BankaHesapOlusturRequest req,
                                           Long userId, Long companyId) {
        if (hesapRepo.existsByHesapKoduAndCompanyId(req.hesapKodu(), companyId)) {
            throw new KasappException(ErrorType.BANKA_HESAP_MEVCUT);
        }

        BankaHesap hesap = BankaHesap.builder()
                .hesapKodu(req.hesapKodu())
                .bankaAdi(req.bankaAdi().toUpperCase())
                .hesapNumarasi(req.hesapNumarasi())
                .baslangicBakiye(req.baslangicBakiye())
                .companyId(companyId)
                .olusturanId(userId)
                .olusturmaTarihi(LocalDateTime.now())
                .aktif(true)
                .build();

        hesapRepo.save(hesap);
        return toHesapResponse(hesap);
    }

    public List<BankaHesapResponse> hesaplariGetir(Long companyId) {
        return hesapRepo.findByCompanyIdAndAktifTrue(companyId)
                .stream()
                .map(this::toHesapResponse)
                .toList();
    }

    public BankaHesapResponse hesapGetir(Long hesapId, Long companyId) {
        return toHesapResponse(getHesap(hesapId, companyId));
    }

    @Transactional
    public void hesapSil(Long hesapId, Long companyId) {
        BankaHesap hesap = getHesap(hesapId, companyId);
        hesap.setAktif(false);
        hesapRepo.save(hesap);
    }

    @Transactional
    public int excelYukle(Long hesapId, Long companyId, Long userId,
                          MultipartFile file) throws IOException {

        BankaHesap hesap = getHesap(hesapId, companyId);
        List<BankaIslem> islemler = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }

                String tarihStr = strVal(row.getCell(0));
                String aciklama = strVal(row.getCell(1));
                String tutarStr = strVal(row.getCell(2));
                String kodStr = strVal(row.getCell(3));

                if (kodStr.isBlank() || tutarStr.isBlank()) {
                    continue;
                }

                // Önce built-in enum'da ara, bulamazsan custom kodlarda ara
                BankaIslemKodu islemKodu = BankaIslemKodu.fromKod(kodStr.trim());
                BankaIslemKodu.Direction direction = null;

                if (islemKodu != null) {
                    direction = islemKodu.getDirection();
                } else {
                    // Custom kod kontrolü
                    var customKod = customKodRepo.findByCompanyId(companyId)
                            .stream()
                            .filter(c -> c.getKod().equals(kodStr.trim()))
                            .findFirst()
                            .orElse(null);
                    if (customKod == null) continue;
                    direction = "IN".equals(customKod.getDirection())
                            ? BankaIslemKodu.Direction.IN
                            : BankaIslemKodu.Direction.OUT;
                }

                BigDecimal tutar;
                try {
                    tutar = new BigDecimal(tutarStr.replace(",", "."));
                } catch (NumberFormatException e) {
                    continue;
                }

                if (tutar.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                islemler.add(BankaIslem.builder()
                        .hesap(hesap)
                        .aciklama(aciklama)
                        .tutar(tutar)
                        .islemKodu(islemKodu)
                        .customKodStr(islemKodu == null ? kodStr.trim() : null)
                        .direction(direction)
                        .islemTarihi(parseDate(tarihStr))
                        .companyId(companyId)
                        .yuklemeYapanId(userId)
                        .yuklemeTarihi(LocalDateTime.now())
                        .build());
            }
        }

        if (!islemler.isEmpty()) {
            islemRepo.saveAll(islemler);
        }
        return islemler.size();
    }

    /**
     * BANKA EKSTRESİ (ham banka excel'i) içe aktarımı.
     * Mevcut elle-şablon akışından (excelYukle) bağımsızdır.
     *  - Tablo başlığını dinamik bulur (İşlem Tarihi / Açıklama / Tutar / Bakiye),
     *    üstteki hesap-bilgisi metadata'sını ve alttaki footer'ı atlar.
     *  - Yönü KODDAN DEĞİL, tutarın İŞARETİNDEN belirler (− çıkış, + giriş);
     *    işaret belirsizse "Yeni Bakiye" farkından çıkarır.
     *  - Türkçe sayı formatını (1.000.000,00) doğru parse eder.
     *  - Kod/kategori atanmaz (Faz 2); işlem koda bakılmadan saklanır.
     */
    @Transactional
    public String importBankaEkstresi(Long hesapId, Long companyId, Long userId,
                                      MultipartFile file) throws IOException {

        BankaHesap hesap = getHesap(hesapId, companyId);
        List<BankaIslem> islemler = new ArrayList<>();
        int atlanan = 0;
        int bakiyeUyari = 0;

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {  // hem .xls hem .xlsx
            Sheet sheet = wb.getSheetAt(0);

            // 1) Tablo başlık satırını ve kolon indekslerini bul — İKİ düzen desteklenir:
            //    (a) Tek "Tutar" kolonu (işaretli ±)
            //    (b) Ayrı "Gelen Havale" (giriş) ve "Gönderilen Havale" (çıkış) kolonları
            int headerRow = -1, cTarih = -1, cAciklama = -1, cTutar = -1, cBakiye = -1,
                cGelen = -1, cGiden = -1, cKod = -1;
            int lastScan = Math.min(sheet.getLastRowNum(), 80);
            for (int r = 0; r <= lastScan; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                int t = -1, a = -1, tu = -1, b = -1, gel = -1, gid = -1, kod = -1;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String v = trNorm(strVal(row.getCell(c)));   // ASCII büyük harf (Türkçe folding)
                    if (v.isBlank()) continue;
                    if (v.contains("TARIH"))                                   t = c;
                    else if (v.contains("CIKLAMA"))                            a = c;   // AÇIKLAMA
                    else if (v.contains("GELEN") || v.contains("ALACAK"))      gel = c; // giriş
                    else if (v.contains("GIDEN") || v.contains("GONDER")
                          || v.contains("BORC"))                              gid = c; // çıkış
                    else if (v.contains("TUTAR"))                              tu = c;
                    else if (v.contains("BAKIYE"))                             b = c;
                    else if (v.contains("KOD"))                                kod = c;  // KODLAR
                }
                // tarih + (tutar VEYA gelen/giden) → başlık satırı
                if (t >= 0 && (tu >= 0 || gel >= 0 || gid >= 0)) {
                    headerRow = r; cTarih = t; cAciklama = a; cTutar = tu; cBakiye = b;
                    cGelen = gel; cGiden = gid; cKod = kod;
                    break;
                }
            }
            if (headerRow < 0) {
                throw new IllegalArgumentException(
                        "Ekstre başlık satırı (Tarih + Tutar/Havale) bulunamadı. Doğru banka dosyası mı yüklediniz?");
            }
            boolean ikiKolon = (cGelen >= 0 || cGiden >= 0);   // yeni format (gelen/giden havale)

            // 1b) Kod kolonu: başlıkta "KODLAR" varsa onu, yoksa GL deseni (NNN.NN.NNN) ile bul
            int codeCol = cKod;
            if (codeCol < 0) {
                Pattern glPattern = Pattern.compile("\\d{3}\\.\\d{2}\\.\\d{3}");
                int[] hits = new int[64];
                for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    int max = Math.min(row.getLastCellNum(), 64);
                    for (int c = 0; c < max; c++) {
                        if (c == cTutar || c == cBakiye || c == cGelen || c == cGiden) continue;
                        String v = strVal(row.getCell(c));
                        if (!v.isBlank() && glPattern.matcher(v).find()) hits[c]++;
                    }
                }
                for (int c = 0; c < 64; c++) {
                    if (hits[c] > 0 && (codeCol < 0 || hits[c] > hits[codeCol])) codeCol = c;
                }
            }

            // Şirkete özel custom kodlar (kategori eşlemesi için)
            Set<String> customKodSet = new HashSet<>();
            customKodRepo.findByCompanyId(companyId)
                    .forEach(ck -> customKodSet.add(ck.getKod()));

            // 2) Veri satırları
            BigDecimal oncekiBakiye = null;
            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String tarihStr = cTarih    >= 0 ? strVal(row.getCell(cTarih))    : "";
                String aciklama = cAciklama >= 0 ? strVal(row.getCell(cAciklama)) : "";
                BigDecimal bakiye = cBakiye >= 0 ? parseTutar(row.getCell(cBakiye)) : null;

                BigDecimal tutar;
                BankaIslemKodu.Direction direction;

                if (ikiKolon) {
                    // Yeni format: dolu olan kolon yönü belirler (gelen=giriş, giden=çıkış)
                    BigDecimal gelen = cGelen >= 0 ? parseTutar(row.getCell(cGelen)) : null;
                    BigDecimal giden = cGiden >= 0 ? parseTutar(row.getCell(cGiden)) : null;
                    boolean gelenVar = gelen != null && gelen.signum() != 0;
                    boolean gidenVar = giden != null && giden.signum() != 0;
                    if (gelenVar)      { tutar = gelen.abs(); direction = BankaIslemKodu.Direction.IN; }
                    else if (gidenVar) { tutar = giden.abs(); direction = BankaIslemKodu.Direction.OUT; }
                    else               { tutar = null;        direction = null; }
                } else {
                    // Eski format: tek işaretli kolon
                    BigDecimal t = parseTutar(row.getCell(cTutar));
                    if (t == null) { tutar = null; direction = null; }
                    else {
                        int sign = t.signum();
                        if (sign < 0)      direction = BankaIslemKodu.Direction.OUT;
                        else if (sign > 0) direction = BankaIslemKodu.Direction.IN;
                        else if (bakiye != null && oncekiBakiye != null)
                            direction = bakiye.compareTo(oncekiBakiye) < 0
                                    ? BankaIslemKodu.Direction.OUT : BankaIslemKodu.Direction.IN;
                        else direction = BankaIslemKodu.Direction.IN;
                        tutar = t.abs();
                    }
                }

                // boş satır / footer
                if (tutar == null && tarihStr.isBlank() && aciklama.isBlank()) continue;
                // tutarı olmayan ara satır (ör. açıklama devamı) → atla
                if (tutar == null) { atlanan++; continue; }

                // Bakiye çapraz doğrulaması (uyarı amaçlı — kayıt yine yapılır)
                if (bakiye != null && oncekiBakiye != null) {
                    BigDecimal beklenenDelta = direction == BankaIslemKodu.Direction.IN
                            ? tutar.abs() : tutar.abs().negate();
                    BigDecimal gercekDelta = bakiye.subtract(oncekiBakiye);
                    if (gercekDelta.subtract(beklenenDelta).abs().compareTo(new BigDecimal("0.05")) > 0) {
                        bakiyeUyari++;
                    }
                }
                if (bakiye != null) oncekiBakiye = bakiye;

                // KATEGORİ: önce dosyadaki GL kodundan (en doğru), yoksa açıklamadan tahmin.
                // NOT: yön burada KULLANILMAZ — yön zaten işaretten geldi.
                BankaIslemKodu kategoriKodu = null;
                String kategoriStr = null;
                String kodStr = (codeCol >= 0) ? strVal(row.getCell(codeCol)).trim() : "";
                if (!kodStr.isBlank()) {
                    kategoriKodu = BankaIslemKodu.fromKod(kodStr);          // built-in kod mu?
                    if (kategoriKodu == null) {
                        kategoriStr = customKodSet.contains(kodStr)         // şirkete özel kod mu?
                                ? kodStr
                                : kategoriOner(aciklama);                   // değilse açıklamadan tahmin
                        if (kategoriStr == null) kategoriStr = kodStr;      // hiç değilse ham kodu sakla
                    }
                } else {
                    kategoriStr = kategoriOner(aciklama);                   // kod yok → açıklamadan tahmin
                }

                islemler.add(BankaIslem.builder()
                        .hesap(hesap)
                        .aciklama(aciklama)
                        .tutar(tutar.abs())
                        .islemKodu(kategoriKodu)
                        .customKodStr(kategoriKodu == null ? kategoriStr : null)
                        .direction(direction)
                        .islemTarihi(parseDate(tarihStr))
                        .companyId(companyId)
                        .yuklemeYapanId(userId)
                        .yuklemeTarihi(LocalDateTime.now())
                        .build());
            }
        }

        if (!islemler.isEmpty()) islemRepo.saveAll(islemler);

        StringBuilder sb = new StringBuilder();
        sb.append(islemler.size()).append(" işlem içe aktarıldı");
        if (atlanan > 0)     sb.append(", ").append(atlanan).append(" satır atlandı");
        if (bakiyeUyari > 0) sb.append(" — ").append(bakiyeUyari)
                .append(" satırda bakiye uyuşmadı, kontrol edin");
        sb.append(".");
        return sb.toString();
    }

    /** Banka tutarı: NUMERIC hücreyi doğrudan, STRING'i Türkçe formatla (1.000.000,00) parse eder. */
    private BigDecimal parseTutar(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String s = strVal(cell);
        if (s == null || s.isBlank()) return null;
        s = s.replaceAll("[^0-9.,-]", "");          // TL, boşluk, simge temizle
        if (s.isBlank() || s.equals("-")) return null;
        if (s.contains(",")) {
            s = s.replace(".", "").replace(",", ".");           // TR: binlik '.', ondalık ','
        } else if (s.chars().filter(ch -> ch == '.').count() > 1) {
            s = s.replace(".", "");                              // çok nokta = binlik ayraç
        }
        try { return new BigDecimal(s); }
        catch (NumberFormatException e) { return null; }
    }

    /** Türkçe karakterleri ASCII'ye indirger (büyük harf) — anahtar kelime eşlemesi için. */
    private String trNorm(String s) {
        if (s == null) return "";
        s = s.toUpperCase(Locale.ROOT);
        return s.replace('İ', 'I').replace('I', 'I').replace('ı', 'I')
                .replace('Ş', 'S').replace('Ç', 'C').replace('Ö', 'O')
                .replace('Ü', 'U').replace('Ğ', 'G');
    }

    /** Açıklamadan kategori tahmini (GL kodu yoksa yedek). Bulunamazsa null → elle atanır. */
    private String kategoriOner(String aciklama) {
        String a = trNorm(aciklama);
        if (a.isBlank()) return null;
        if (a.contains("KOMISYON"))                              return "Komisyon";
        if (a.contains("FAIZ"))                                  return "Faiz";
        if (a.contains("TAHSIS"))                                return "Kredi Tahsis Ücreti";
        if (a.contains("KULLANDIRIM") || a.contains("KREDI KULL")) return "Kredi Kullandırımı";
        if (a.contains("KAPANIS"))                               return "Kredi Kapanışı";
        if (a.contains("UCRET") || a.contains("MASRAF"))         return "Banka Masrafı/Ücret";
        if (a.contains("EFT"))                                   return "EFT";
        if (a.contains("HAVALE") || a.contains("FAST"))          return "Havale/FAST";
        if (a.contains("VERGI") || a.contains("SGK"))            return "Vergi/SGK";
        if (a.contains("MAAS"))                                  return "Maaş";
        return null;
    }

    public List<BankaIslemResponse> islemleriGetir(Long hesapId, Long companyId,
                                                   int page, int size) {
        getHesap(hesapId, companyId);
        PageRequest pr = PageRequest.of(page, size);
        return islemRepo.findRowsByHesapIdAndCompanyId(hesapId, companyId, pr)
                .stream()
                .map(this::toIslemResponse)
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public int islemleriTemizle(Long hesapId, Long companyId) {
        getHesap(hesapId, companyId);
        int silinen = Math.toIntExact(islemRepo.countByHesapIdAndCompanyId(hesapId, companyId));
        islemRepo.deleteByHesapIdAndCompanyId(hesapId, companyId);
        return silinen;
    }

    @Transactional
    public void islemSil(Long hesapId, Long islemId, Long companyId) {
        getHesap(hesapId, companyId);
        BankaIslem islem = islemRepo.findByIdAndHesapIdAndCompanyId(islemId, hesapId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.BANKA_HESAP_BULUNAMADI));
        islemRepo.delete(islem);
    }

    /** Bir banka işleminin kategorisini (kodunu) elle değiştirir. Yön DEĞİŞMEZ (işaretten gelir). */
    @Transactional
    public void updateIslemKategori(Long hesapId, Long islemId, Long companyId, String kodStr) {
        getHesap(hesapId, companyId);
        BankaIslem islem = islemRepo.findByIdAndHesapIdAndCompanyId(islemId, hesapId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.BANKA_HESAP_BULUNAMADI));

        if (kodStr == null || kodStr.isBlank()) {
            islem.setIslemKodu(null);
            islem.setCustomKodStr(null);
        } else {
            String kod = kodStr.trim();
            BankaIslemKodu builtin = BankaIslemKodu.fromKod(kod);
            if (builtin != null) {
                islem.setIslemKodu(builtin);
                islem.setCustomKodStr(null);
            } else {
                islem.setIslemKodu(null);
                islem.setCustomKodStr(kod);   // şirkete özel kod veya serbest kategori
            }
        }
        islemRepo.save(islem);
    }

    /** Kategori bazında giriş/çıkış/net özeti (opsiyonel tarih aralığı: yyyy-MM-dd). */
    public List<BankaKategoriOzet> kategoriOzet(Long hesapId, Long companyId, String start, String end) {
        getHesap(hesapId, companyId);
        LocalDate bas = parseStoredDate(start);
        LocalDate bit = parseStoredDate(end);

        Map<String, BigDecimal[]> giriCikis = new LinkedHashMap<>();  // ad -> [giris, cikis]
        Map<String, long[]>       sayac     = new LinkedHashMap<>();
        Map<String, String>       kodlar    = new LinkedHashMap<>();

        for (BankaIslem i : islemRepo.findByHesapIdAndCompanyId(hesapId, companyId)) {
            LocalDate d = i.getIslemTarihi();
            if (bas != null && d != null && d.isBefore(bas)) continue;
            if (bit != null && d != null && d.isAfter(bit))  continue;

            String ad, kod;
            if (i.getIslemKodu() != null) {
                ad  = i.getIslemKodu().getAciklama();
                kod = i.getIslemKodu().getKod();
            } else if (i.getCustomKodStr() != null && !i.getCustomKodStr().isBlank()) {
                ad  = i.getCustomKodStr();
                kod = i.getCustomKodStr();
            } else {
                ad  = "Kategorisiz";
                kod = "";
            }

            giriCikis.computeIfAbsent(ad, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            sayac.computeIfAbsent(ad, k -> new long[]{0});
            kodlar.putIfAbsent(ad, kod);

            BigDecimal tutar = i.getTutar() != null ? i.getTutar() : BigDecimal.ZERO;
            if (i.getDirection() == BankaIslemKodu.Direction.IN) {
                giriCikis.get(ad)[0] = giriCikis.get(ad)[0].add(tutar);
            } else {
                giriCikis.get(ad)[1] = giriCikis.get(ad)[1].add(tutar);
            }
            sayac.get(ad)[0]++;
        }

        List<BankaKategoriOzet> sonuc = new ArrayList<>();
        for (var e : giriCikis.entrySet()) {
            BigDecimal giris = e.getValue()[0];
            BigDecimal cikis = e.getValue()[1];
            sonuc.add(new BankaKategoriOzet(
                    e.getKey(), kodlar.get(e.getKey()),
                    giris, cikis, giris.subtract(cikis), sayac.get(e.getKey())[0]));
        }
        sonuc.sort((a, b) -> b.net().abs().compareTo(a.net().abs()));
        return sonuc;
    }

    /** Built-in enum kodları + şirkete özel custom kodları birleştirerek döner */
    public List<BankaIslemKoduResponse> islemKodlariGetir(Long companyId) {
        List<BankaIslemKoduResponse> builtIn = Arrays.stream(BankaIslemKodu.values())
                .map(k -> new BankaIslemKoduResponse(null, k.getKod(), k.getAciklama(), k.getDirection().name()))
                .toList();

        List<BankaIslemKoduResponse> custom = customKodRepo.findByCompanyId(companyId)
                .stream()
                .map(k -> new BankaIslemKoduResponse(k.getId(), k.getKod(), k.getAciklama(), k.getDirection()))
                .toList();

        return Stream.concat(builtIn.stream(), custom.stream()).toList();
    }

    @Transactional
    public BankaIslemKoduCustom islemKoduEkle(String kod, String aciklama,
                                               String direction, Long companyId) {
        // Built-in enum ile çakışma kontrolü
        if (BankaIslemKodu.fromKod(kod.trim()) != null) {
            throw new KasappException(ErrorType.VALIDATION_ERROR);
        }
        if (customKodRepo.existsByKodAndCompanyId(kod.trim(), companyId)) {
            throw new KasappException(ErrorType.VALIDATION_ERROR);
        }
        String dir = "IN".equalsIgnoreCase(direction) ? "IN" : "OUT";
        BankaIslemKoduCustom entity = BankaIslemKoduCustom.builder()
                .kod(kod.trim())
                .aciklama(aciklama != null ? aciklama.trim() : "")
                .direction(dir)
                .companyId(companyId)
                .createdAt(LocalDateTime.now())
                .build();
        return customKodRepo.save(entity);
    }

    @Transactional
    public void islemKoduSil(Long id, Long companyId) {
        BankaIslemKoduCustom entity = customKodRepo.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.BANKA_HESAP_BULUNAMADI));
        customKodRepo.delete(entity);
    }

    private BankaHesap getHesap(Long hesapId, Long companyId) {
        return hesapRepo.findByIdAndCompanyId(hesapId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.BANKA_HESAP_BULUNAMADI));
    }

    private BigDecimal guncelBakiye(BankaHesap hesap) {
        BigDecimal net = islemRepo.netHareket(hesap.getId());
        return hesap.getBaslangicBakiye().add(net == null ? BigDecimal.ZERO : net);
    }

    private BankaHesapResponse toHesapResponse(BankaHesap hesap) {
        return new BankaHesapResponse(
                hesap.getId(),
                hesap.getHesapKodu(),
                hesap.getBankaAdi(),
                hesap.getHesapNumarasi(),
                hesap.getBaslangicBakiye(),
                guncelBakiye(hesap),
                hesap.getOlusturmaTarihi());
    }

    private BankaIslemResponse toIslemResponse(BankaIslemRepository.BankaIslemRow row) {
        BankaIslemKodu islemKodu = parseIslemKodu(row.getIslemKoduRaw());

        // Enum'da bulunamadı — custom kod (direction DB'den okunur)
        if (islemKodu == null) {
            String rawDir       = row.getDirectionRaw();
            String customKodStr = row.getCustomKodStrRaw();

            if (rawDir == null || rawDir.isBlank()) {
                log.warn("Banka islem row skipped — no direction. id={}", row.getId());
                return null;
            }
            String dir = rawDir.trim().toUpperCase();
            if (!dir.equals("IN") && !dir.equals("OUT")) {
                log.warn("Banka islem row skipped — invalid direction. id={}, dir={}", row.getId(), dir);
                return null;
            }

            String displayKod = (customKodStr != null && !customKodStr.isBlank()) ? customKodStr : "—";
            return new BankaIslemResponse(
                    row.getId(),
                    row.getAciklama(),
                    row.getTutar(),
                    displayKod,
                    displayKod,
                    dir,
                    parseStoredDate(row.getIslemTarihiRaw()));
        }

        BankaIslemKodu.Direction direction = parseDirection(row.getDirectionRaw(), islemKodu);
        return new BankaIslemResponse(
                row.getId(),
                row.getAciklama(),
                row.getTutar(),
                islemKodu.getAciklama(),
                islemKodu.getKod(),
                direction.name(),
                parseStoredDate(row.getIslemTarihiRaw()));
    }

    private String strVal(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }

        String[] patterns = {
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "dd.MM.yyyy",
                "yyyy-MM-dd",
                "d-M-yyyy",
                "d/M/yyyy",
                "d.M.yyyy",
                "M/d/yyyy",   // ABD tipi (yeni .xls)
                "M/d/yy",     // 5/1/26
                "MM/dd/yy"
        };

        for (String pattern : patterns) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
            }
        }

        return LocalDate.now();
    }

    private LocalDate parseStoredDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return parseDate(value);
        }
    }

    private BankaIslemKodu parseIslemKodu(String rawKod) {
        if (rawKod == null || rawKod.isBlank()) {
            return null;
        }

        try {
            return BankaIslemKodu.valueOf(rawKod.trim());
        } catch (IllegalArgumentException ignored) {
            return BankaIslemKodu.fromKod(rawKod);
        }
    }

    private BankaIslemKodu.Direction parseDirection(String rawDirection, BankaIslemKodu islemKodu) {
        if (rawDirection != null && !rawDirection.isBlank()) {
            try {
                return BankaIslemKodu.Direction.valueOf(rawDirection.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return islemKodu.getDirection();
    }
}
