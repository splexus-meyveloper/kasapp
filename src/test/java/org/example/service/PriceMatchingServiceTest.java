package org.example.service;

import org.example.entity.PriceCodeCrossReference;
import org.example.entity.PriceImportRow;
import org.example.entity.PriceSupplier;
import org.example.entity.StockSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceMatchingServiceTest {

    private final PriceMatchingService matcher = new PriceMatchingService();
    private final PriceSupplier supplier = PriceSupplier.builder().id(1L).build();

    @Test
    void ureticiMalKoduAlaniIleDogrudanEslesir() {
        // Gerçek CPM örneği: Mal Kodu "T5560601H", Üretici Mal Kodu "H5560601 P M"
        PriceImportRow row = PriceImportRow.builder()
                .manufacturerCode(PriceExcelParseService.normalizeCode("H5560601 P M"))
                .listPrice(BigDecimal.valueOf(1000))
                .build();

        StockSnapshot snapshot = StockSnapshot.builder()
                .stockCode("T5560601H")
                .manufacturerCode("H5560601 P M") // ham haliyle saklanır, eşleştirmede normalize edilir
                .build();

        var results = matcher.match(List.of(row), List.of(snapshot), supplier, List.of());

        assertTrue(results.get(0).isMatched());
        assertEquals("URETICI_MAL_KODU", results.get(0).matchSource());
        assertEquals(snapshot, results.get(0).snapshot());
    }

    @Test
    void tekHucredeBirdenFazlaUreticiKoduBosluklaAyriliyorsaHerBiriEslesir() {
        // Gerçek CPM örneği: Üretici Mal Kodu hücresi "10104    111005" (iki kod birden)
        PriceImportRow row1 = PriceImportRow.builder().manufacturerCode("10104").listPrice(BigDecimal.TEN).build();
        PriceImportRow row2 = PriceImportRow.builder().manufacturerCode("111005").listPrice(BigDecimal.TEN).build();

        StockSnapshot snapshot = StockSnapshot.builder()
                .stockCode("AIR111015")
                .manufacturerCode("10104    111005")
                .build();

        var results = matcher.match(List.of(row1, row2), List.of(snapshot), supplier, List.of());

        assertTrue(results.get(0).isMatched());
        assertTrue(results.get(1).isMatched());
    }

    @Test
    void manufacturerCode2Ve3AlanlariDaDenenir() {
        PriceImportRow row = PriceImportRow.builder().manufacturerCode("111003").listPrice(BigDecimal.ONE).build();
        StockSnapshot snapshot = StockSnapshot.builder()
                .stockCode("X1").manufacturerCode("10104").manufacturerCode2("111003").build();

        var results = matcher.match(List.of(row), List.of(snapshot), supplier, List.of());

        assertTrue(results.get(0).isMatched());
    }

    @Test
    void stokKoduDogrudanVarsaUreticiMalKodundanOncelikli() {
        PriceImportRow row = PriceImportRow.builder()
                .manufacturerCode("XYZ") // CPM tarafında hiç yok
                .stockCode(PriceExcelParseService.normalizeCode("152.070.009"))
                .listPrice(BigDecimal.TEN)
                .build();

        StockSnapshot snapshot = StockSnapshot.builder()
                .stockCode(PriceExcelParseService.normalizeCode("152.070.009"))
                .build();

        var results = matcher.match(List.of(row), List.of(snapshot), supplier, List.of());

        assertTrue(results.get(0).isMatched());
    }

    @Test
    void hafizadaKayitliEslesmeUreticiMalKoduAlaniBosOlsaBileCalisir() {
        // CPM'de bu üründe Üretici Mal Kodu alanı BOŞ (kullanıcının belirttiği ~%10'luk grup) —
        // ama daha önce elle eşleştirilmiş ve hafızaya (xref) yazılmış.
        PriceImportRow row = PriceImportRow.builder().manufacturerCode("ESKI-KOD-123").listPrice(BigDecimal.TEN).build();
        StockSnapshot snapshot = StockSnapshot.builder().stockCode("KS100.30.07.U").manufacturerCode(null).build();

        PriceCodeCrossReference xref = PriceCodeCrossReference.builder()
                .companyId(1L).supplierId(1L).manufacturerCode("ESKI-KOD-123").stockCode("KS100.30.07.U").source("MANUAL")
                .build();

        var results = matcher.match(List.of(row), List.of(snapshot), supplier, List.of(xref));

        assertTrue(results.get(0).isMatched());
        assertEquals("HAFIZA", results.get(0).matchSource());
        assertEquals(snapshot, results.get(0).snapshot());
    }

    @Test
    void tanimliOnEkTemizlenipTekrarDenenir() {
        // Kullanıcının verdiği gerçek örnek: üretici kodu "US 206103 K", CPM'de "206103 K"
        PriceSupplier supplierWithPrefix = PriceSupplier.builder().id(1L).ignoredCodePrefixes("US").build();

        PriceImportRow row = PriceImportRow.builder()
                .manufacturerCode(PriceExcelParseService.normalizeCode("US 206103 K"))
                .listPrice(BigDecimal.TEN)
                .build();

        StockSnapshot snapshot = StockSnapshot.builder()
                .stockCode("SOME-STOCK-CODE")
                .manufacturerCode("206103 K")
                .build();

        var results = matcher.match(List.of(row), List.of(snapshot), supplierWithPrefix, List.of());

        assertTrue(results.get(0).isMatched(), "US ön eki temizlenince eşleşmeli");
        assertEquals("URETICI_MAL_KODU_ONEK_TEMIZ", results.get(0).matchSource());
    }

    @Test
    void onEkCpmTarafindaysaTersYondeEklenerekEslesir() {
        // USMER gerçek örneği: üretici listesinde çıplak kod "2757", CPM'in kendi
        // Mal Kodu alanında ise "US2757" (CPM'in kendi numaralandırması ön eki taşıyor).
        PriceSupplier supplierWithPrefix = PriceSupplier.builder().id(1L).ignoredCodePrefixes("US").build();

        PriceImportRow row = PriceImportRow.builder()
                .manufacturerCode(PriceExcelParseService.normalizeCode("2757"))
                .listPrice(BigDecimal.TEN)
                .build();

        StockSnapshot snapshot = StockSnapshot.builder()
                .stockCode("US 2757")
                .manufacturerCode("US2757")
                .build();

        var results = matcher.match(List.of(row), List.of(snapshot), supplierWithPrefix, List.of());

        assertTrue(results.get(0).isMatched(), "CPM tarafındaki US ön eki eklenerek eşleşmeli");
        assertEquals("URETICI_MAL_KODU_ONEK_EKLE", results.get(0).matchSource());
    }

    @Test
    void onEkTanimliDegilseDogrudanEslesmeyenSatirYineEslesmez() {
        PriceImportRow row = PriceImportRow.builder()
                .manufacturerCode(PriceExcelParseService.normalizeCode("US 206103 K"))
                .listPrice(BigDecimal.TEN)
                .build();
        StockSnapshot snapshot = StockSnapshot.builder().stockCode("X").manufacturerCode("206103 K").build();

        var results = matcher.match(List.of(row), List.of(snapshot), supplier, List.of()); // ön ek tanımsız supplier

        assertFalse(results.get(0).isMatched());
    }

    @Test
    void hicbirSeyleEslesmeyenSatirRaporaDuser() {
        PriceImportRow row = PriceImportRow.builder().manufacturerCode("BILINMEYEN").listPrice(BigDecimal.ONE).build();

        var results = matcher.match(List.of(row), List.of(), supplier, List.of());

        assertFalse(results.get(0).isMatched());
    }
}
