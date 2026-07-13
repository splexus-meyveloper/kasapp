package org.example.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entity.PriceImportRow;
import org.example.entity.PriceImportTemplate;
import org.example.entity.PriceSupplier;
import org.example.entity.StockSnapshot;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kullanıcının paylaştığı gerçek örnek dosyalarla uçtan uca doğrulama.
 * Dosyalar bu makinede yoksa test kendini otomatik atlar (CI/başka makine
 * için bir sorun oluşturmaz) — sadece bu geliştirme ortamında anlamlıdır.
 */
class PriceExcelParseServiceRealFileTest {

    private static final Path MANUFACTURER_FILE =
            Path.of("C:/Users/ETICARET/Desktop/Mayıs 2026 Ticari Grup Fiyat Listesi.xlsx");
    private static final Path CPM_FULL_FILE =
            Path.of("C:/Users/ETICARET/Desktop/CPM STOK KARTLARI.xlsx");
    private static final Path USMER_CPM_FILE =
            Path.of("C:/Users/ETICARET/Desktop/USMER TÜM CPM LİSTE.xls");
    private static final Path USMER_MANUFACTURER_FILE =
            Path.of("C:/Users/ETICARET/Desktop/Ürünler (5).xlsx");

    private final PriceExcelParseService parseService = new PriceExcelParseService();
    private final PriceMatchingService matchingService = new PriceMatchingService();

    @Test
    void gercekUreticiListesiDogruParseEdilir() throws Exception {
        Assumptions.assumeTrue(Files.exists(MANUFACTURER_FILE), "Örnek dosya bu makinede yok, atlanıyor");

        PriceImportTemplate template = PriceImportTemplate.builder().headerRowIndex(1).build();
        Map<String, Integer> mapping = Map.of("MANUFACTURER_CODE", 2, "DESCRIPTION", 1, "LIST_PRICE", 6);

        List<PriceImportRow> rows;
        try (InputStream in = new FileInputStream(MANUFACTURER_FILE.toFile());
             Workbook wb = new XSSFWorkbook(in)) {
            rows = parseService.parseManufacturerList(wb, template, 1L, mapping);
        }

        assertFalse(rows.isEmpty());
        assertTrue(rows.size() > 700, "700'den fazla satır bekleniyor, gelen: " + rows.size());

        // İlk gerçek veri satırı: MARKA=BMC, MODEL=BELDE, MAYSAN MANDO NO="N6750501 P M", ALIŞ=1.383,00
        PriceImportRow first = rows.get(0);
        assertEquals("N6750501PM", first.getManufacturerCode());
        assertEquals(0, new BigDecimal("1383.0000").compareTo(first.getListPrice()));
        assertEquals("BELDE", first.getDescription());
        assertEquals("TRY", first.getCurrencyCode());
    }

    @Test
    void gercekCpmTamExportuDogruParseEdilir_UreticiKoduVeGrup() throws Exception {
        Assumptions.assumeTrue(Files.exists(CPM_FULL_FILE), "Örnek dosya bu makinede yok, atlanıyor");

        // Gerçek kolonlar: 1=Mal Kodu 2=Mal Adı 6=Araç Türü 15=Satış1 16=Satış3 17=Satış4
        // 21=Üretici Mal Kodu 22=Üretici Mal Kodu 2 23=Üretici Mal Kodu 3 (1-tabanlı -> 0-tabanlıya çevrildi)
        PriceImportTemplate template = PriceImportTemplate.builder().headerRowIndex(0).build();
        Map<String, Integer> mapping = Map.of(
                "STOCK_CODE", 0, "DESCRIPTION", 1, "PRODUCT_GROUP", 5,
                "SALES1", 14, "SALES3", 15, "SALES4", 16,
                "MANUFACTURER_CODE", 20, "MANUFACTURER_CODE_2", 21, "MANUFACTURER_CODE_3", 22
        );

        List<StockSnapshot> rows;
        try (InputStream in = new FileInputStream(CPM_FULL_FILE.toFile()); Workbook wb = new XSSFWorkbook(in)) {
            rows = parseService.parseStock(wb, template, 1L, mapping);
        }

        assertTrue(rows.size() > 13000, "13000'den fazla ürün bekleniyor, gelen: " + rows.size());

        long ticari = rows.stream().filter(r -> "Ticari Grup".equals(r.getVehicleGroup())).count();
        long binek = rows.stream().filter(r -> "Binek Grup".equals(r.getVehicleGroup())).count();
        assertTrue(ticari > 12000, "Ticari Grup sayımı beklenenden düşük: " + ticari);
        assertTrue(binek > 500, "Binek Grup sayımı beklenenden düşük: " + binek);

        long withMfgCode = rows.stream().filter(r -> r.getManufacturerCode() != null).count();
        assertTrue(withMfgCode > 11000, "Üretici Mal Kodu doluluk oranı beklenenden düşük: " + withMfgCode);

        // İlk satır: AIR 101169K | ... | Üretici Mal Kodu = 11201001
        StockSnapshot first = rows.get(0);
        assertEquals("AIR101169K", first.getStockCode());
        assertEquals("Ticari Grup", first.getVehicleGroup());
        assertEquals("11201001", first.getManufacturerCode());
    }

    @Test
    void cokluUreticiKoduHucresiHerIkiKoduDaEslestirir() throws Exception {
        Assumptions.assumeTrue(Files.exists(CPM_FULL_FILE), "Örnek dosya bu makinede yok, atlanıyor");

        PriceImportTemplate template = PriceImportTemplate.builder().headerRowIndex(0).build();
        Map<String, Integer> mapping = Map.of("STOCK_CODE", 0, "MANUFACTURER_CODE", 20, "MANUFACTURER_CODE_2", 21);

        List<StockSnapshot> rows;
        try (InputStream in = new FileInputStream(CPM_FULL_FILE.toFile()); Workbook wb = new XSSFWorkbook(in)) {
            rows = parseService.parseStock(wb, template, 1L, mapping);
        }

        // 5. satır (AIR 111015): Üretici Mal Kodu hücresi "10104    111005" (iki kod birden, boşlukla ayrık)
        StockSnapshot air111015 = rows.stream().filter(r -> "AIR111015".equals(r.getStockCode())).findFirst().orElseThrow();
        assertTrue(air111015.getManufacturerCode().contains("10104"));
        assertTrue(air111015.getManufacturerCode().contains("111005"));

        PriceImportRow rowA = PriceImportRow.builder().manufacturerCode("10104").listPrice(BigDecimal.ONE).build();
        PriceImportRow rowB = PriceImportRow.builder().manufacturerCode("111005").listPrice(BigDecimal.ONE).build();

        var results = matchingService.match(List.of(rowA, rowB), rows, PriceSupplier.builder().id(1L).build(), List.of());

        assertTrue(results.get(0).isMatched(), "10104 eşleşmeli");
        assertTrue(results.get(1).isMatched(), "111005 eşleşmeli");
        assertEquals(air111015.getId(), results.get(0).snapshot().getId());
        assertEquals(air111015.getId(), results.get(1).snapshot().getId());
    }

    /**
     * USMER gerçek örneği: CPM'nin kendi "Mal Kodu" alanı ("US 2757") tedarikçinin
     * kendi kodunun ("2757") başına "US" ekleyerek oluşturuluyor — "Üretici Mal Kodu"
     * alanı (152.072.252.P gibi) bambaşka, alakasız bir referans numarası. Bu yüzden
     * eşleştirme için CPM tarafında MANUFACTURER_CODE alanına "Üretici Mal Kodu" değil
     * "Mal Kodu" kolonunun kendisi verilmeli; ignoredCodePrefixes="US" ile ters yönde
     * (PriceMatchingService adım 5) eşleşme sağlanır. Ayrıca USMER dosyası eski .xls
     * (BIFF/HSSF) formatında — WorkbookFactory'nin bunu da okuyabildiğini doğrular.
     */
    @Test
    void usmerGercekDosyalariOnEkTersYonEslesmesiyleCogunluklaEslesir() throws Exception {
        Assumptions.assumeTrue(Files.exists(USMER_CPM_FILE), "Örnek dosya bu makinede yok, atlanıyor");
        Assumptions.assumeTrue(Files.exists(USMER_MANUFACTURER_FILE), "Örnek dosya bu makinede yok, atlanıyor");

        PriceImportTemplate cpmTemplate = PriceImportTemplate.builder().headerRowIndex(0).build();
        Map<String, Integer> cpmMapping = Map.of(
                "STOCK_CODE", 0, "DESCRIPTION", 1, "PRODUCT_GROUP", 2,
                "SALES1", 3, "SALES3", 4, "SALES4", 5,
                "MANUFACTURER_CODE", 0 // "Üretici Mal Kodu" değil, "Mal Kodu" kolonunun kendisi
        );
        List<StockSnapshot> stock;
        try (InputStream in = new FileInputStream(USMER_CPM_FILE.toFile());
             Workbook wb = WorkbookFactory.create(in)) { // .xls -> HSSF, otomatik algılanır
            stock = parseService.parseStock(wb, cpmTemplate, 1L, cpmMapping);
        }
        assertTrue(stock.size() > 300, "USMER CPM listesinde 300+ satır bekleniyor, gelen: " + stock.size());

        PriceImportTemplate mfgTemplate = PriceImportTemplate.builder().headerRowIndex(1).build();
        Map<String, Integer> mfgMapping = Map.of("MANUFACTURER_CODE", 1, "LIST_PRICE", 2, "CURRENCY", 3);
        List<PriceImportRow> mfgRows;
        try (InputStream in = new FileInputStream(USMER_MANUFACTURER_FILE.toFile());
             Workbook wb = WorkbookFactory.create(in)) {
            mfgRows = parseService.parseManufacturerList(wb, mfgTemplate, 2L, mfgMapping);
        }
        assertTrue(mfgRows.size() > 1300, "Ürünler listesinde 1300+ satır bekleniyor, gelen: " + mfgRows.size());

        PriceSupplier usmer = PriceSupplier.builder().id(1L).ignoredCodePrefixes("US").build();
        var results = matchingService.match(mfgRows, stock, usmer, List.of());

        long matched = results.stream().filter(PriceMatchingService.MatchResult::isMatched).count();
        long viaPrefixAdd = results.stream().filter(r -> "URETICI_MAL_KODU_ONEK_EKLE".equals(r.matchSource())).count();

        assertTrue(matched > 300, "US ön eki ters yönde eklenince 300+ eşleşme bekleniyor, gelen: " + matched);
        assertTrue(viaPrefixAdd > 0, "Eşleşmelerin en azından bir kısmı ONEK_EKLE üzerinden gelmeli");
    }
}
