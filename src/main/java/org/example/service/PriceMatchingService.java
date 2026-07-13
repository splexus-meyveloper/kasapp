package org.example.service;

import org.example.entity.PriceCodeCrossReference;
import org.example.entity.PriceImportRow;
import org.example.entity.PriceSupplier;
import org.example.entity.StockSnapshot;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Üretici fiyat listesi satırlarını CPM stok listesiyle eşleştirir.
 *
 * Öncelik sırası:
 *   1) Kalıcı eşleştirme hafızası (PriceCodeCrossReference) — daha önce
 *      (otomatik veya elle) eşleştirilmiş bir ürün bir daha asla sorulmaz.
 *   2) Stok kodu (üretici listesi doğrudan CPM kodunu da veriyorsa).
 *   3) CPM'in "Üretici Mal Kodu" / 2 / 3 alanları — tek hücrede birden
 *      fazla kod boşlukla ayrılmış olabilir (örn. "10104    111005"),
 *      bu yüzden burada bölünüp her biri ayrı anahtar olarak indekslenir.
 *   4) Yukarıdaki doğrudan eşleşme tutmazsa, tedarikçiye tanımlı "yok
 *      sayılacak ön ek"ler (örn. "US") üretici kodundan temizlenip tekrar
 *      denenir — kullanıcı bunu bir kez tanımlar, sonrasında otomatik uygulanır.
 *   5) Ön ek ters yönde de denenir: bazı tedarikçilerde ön eki üretici listesi
 *      değil, CPM'in kendi Mal Kodu/Üretici Mal Kodu alanı taşır (örn. üretici
 *      listesinde "2757", CPM'de "US2757") — bu durumda ön ek üretici koduna
 *      EKLENEREK CPM indeksinde aranır.
 *
 * Not: CPM'in kendi stok kodu (Mal Kodu) formatı tutarsız (marka/ürün
 * ailesine göre farklı isimlendirme kullanılıyor) — bu yüzden üretici
 * kodundan CPM koduna regex ile "tahmin" YAPILMAZ; eşleşmeyenler rapora
 * düşer ve kullanıcı elle eşleştirir (bu da hafızaya kalıcı yazılır).
 */
@Service
public class PriceMatchingService {

    public record MatchResult(PriceImportRow row, StockSnapshot snapshot, String matchSource) {
        public boolean isMatched() { return snapshot != null; }
    }

    public List<MatchResult> match(List<PriceImportRow> rows, List<StockSnapshot> snapshots,
                                   PriceSupplier supplier, List<PriceCodeCrossReference> xrefs) {
        Map<String, StockSnapshot> byStockCode = new HashMap<>();
        Map<String, StockSnapshot> byManufacturerCode = new HashMap<>();
        for (StockSnapshot s : snapshots) {
            if (s.getStockCode() != null) byStockCode.putIfAbsent(s.getStockCode(), s);
            indexManufacturerCodes(s, s.getManufacturerCode(), byManufacturerCode);
            indexManufacturerCodes(s, s.getManufacturerCode2(), byManufacturerCode);
            indexManufacturerCodes(s, s.getManufacturerCode3(), byManufacturerCode);
        }

        Map<String, String> xrefByCode = new HashMap<>();
        for (PriceCodeCrossReference x : xrefs) xrefByCode.put(x.getManufacturerCode(), x.getStockCode());

        List<String> ignoredPrefixes = parsePrefixes(supplier.getIgnoredCodePrefixes());

        return rows.stream().map(row -> {
            String mfgCode = row.getManufacturerCode();

            // 1) Hafıza
            String rememberedStockCode = xrefByCode.get(mfgCode);
            if (rememberedStockCode != null) {
                StockSnapshot s = byStockCode.get(rememberedStockCode);
                if (s != null) return new MatchResult(row, s, "HAFIZA");
            }

            // 2) Stok kodu (üretici listesi doğrudan verdiyse)
            if (row.getStockCode() != null) {
                StockSnapshot bySc = byStockCode.get(PriceExcelParseService.normalizeCode(row.getStockCode()));
                if (bySc != null) return new MatchResult(row, bySc, "STOK_KODU");
            }

            // 3) CPM Üretici Mal Kodu alanları — doğrudan
            StockSnapshot byMc = byManufacturerCode.get(mfgCode);
            if (byMc != null) return new MatchResult(row, byMc, "URETICI_MAL_KODU");

            // 4) Tedarikçiye tanımlı ön ekler temizlenip tekrar denenir (örn. "US206103K" -> "206103K")
            for (String prefix : ignoredPrefixes) {
                if (mfgCode != null && mfgCode.startsWith(prefix)) {
                    StockSnapshot byStripped = byManufacturerCode.get(mfgCode.substring(prefix.length()));
                    if (byStripped != null) return new MatchResult(row, byStripped, "URETICI_MAL_KODU_ONEK_TEMIZ");
                }
            }

            // 5) Ters yön: ön eki CPM tarafı taşıyor (örn. üretici listesi "2757", CPM "US2757")
            if (mfgCode != null) {
                for (String prefix : ignoredPrefixes) {
                    StockSnapshot byPrefixed = byManufacturerCode.get(prefix + mfgCode);
                    if (byPrefixed != null) return new MatchResult(row, byPrefixed, "URETICI_MAL_KODU_ONEK_EKLE");
                }
            }

            return new MatchResult(row, null, null);
        }).toList();
    }

    private List<String> parsePrefixes(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return java.util.Arrays.stream(raw.split(","))
                .map(p -> PriceExcelParseService.normalizeCode(p))
                .filter(p -> !p.isBlank())
                .toList();
    }

    /**
     * Bir hücrede birden fazla üretici kodu birbirinden GENİŞ boşlukla (2+ karakter)
     * ayrılmış olabilir (örn. "10104    111005" — iki farklı kod). Tek boşluk ise
     * üreticinin kendi kod formatının bir parçasıdır (örn. "H5560601 P M" tek bir
     * kod) — bu yüzden yalnızca 2+ boşlukta bölünür, kalan tekli boşluklar
     * normalizeCode ile temizlenir.
     */
    private void indexManufacturerCodes(StockSnapshot s, String raw, Map<String, StockSnapshot> index) {
        if (raw == null || raw.isBlank()) return;
        for (String token : raw.trim().split("\\s{2,}")) {
            String normalized = PriceExcelParseService.normalizeCode(token);
            if (!normalized.isBlank()) index.putIfAbsent(normalized, s);
        }
    }
}
