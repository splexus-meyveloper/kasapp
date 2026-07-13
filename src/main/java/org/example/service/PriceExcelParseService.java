package org.example.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.example.entity.PriceImportRow;
import org.example.entity.PriceImportTemplate;
import org.example.entity.StockSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel'i şablondaki kolon eşlemesine göre parse eder — kolon yapısı kod
 * içine gömülü değil, PriceImportTemplate.fieldMappingsJson üzerinden gelir.
 * Field anahtarları: MANUFACTURER_CODE, STOCK_CODE, DESCRIPTION, LIST_PRICE,
 * CURRENCY, BARCODE, SALES1, SALES2, SALES3, SALES4.
 */
@Service
public class PriceExcelParseService {

    /** Önizleme için — ilk N satırı ham metin olarak döner (kolon eşleme ekranında gösterilir). */
    public List<List<String>> previewRows(Workbook wb, int maxRows) {
        Sheet sheet = wb.getSheetAt(0);
        List<List<String>> out = new ArrayList<>();
        int last = Math.min(sheet.getLastRowNum(), maxRows - 1);
        for (int r = 0; r <= last; r++) {
            Row row = sheet.getRow(r);
            List<String> cells = new ArrayList<>();
            if (row != null) {
                int maxCol = Math.min(row.getLastCellNum(), 40);
                for (int c = 0; c < maxCol; c++) {
                    cells.add(strVal(row.getCell(c)));
                }
            }
            out.add(cells);
        }
        return out;
    }

    public List<PriceImportRow> parseManufacturerList(Workbook wb, PriceImportTemplate template,
                                                       Long batchId, Map<String, Integer> mapping) {
        Sheet sheet = wb.getSheetAt(0);
        List<PriceImportRow> rows = new ArrayList<>();

        Integer codeCol = mapping.get("MANUFACTURER_CODE");
        Integer stockCol = mapping.get("STOCK_CODE");
        Integer descCol = mapping.get("DESCRIPTION");
        Integer priceCol = mapping.get("LIST_PRICE");
        Integer currencyCol = mapping.get("CURRENCY");
        if (codeCol == null || priceCol == null) {
            throw new IllegalArgumentException("Şablonda MANUFACTURER_CODE ve LIST_PRICE eşlemesi zorunludur.");
        }

        for (int r = template.getHeaderRowIndex() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String code = strVal(row.getCell(codeCol));
            if (code.isBlank()) continue;

            BigDecimal price = numVal(row.getCell(priceCol));
            if (price == null) continue;

            rows.add(PriceImportRow.builder()
                    .batchId(batchId)
                    .manufacturerCode(normalizeCode(code))
                    .stockCode(stockCol != null ? emptyToNull(strVal(row.getCell(stockCol))) : null)
                    .description(descCol != null ? strVal(row.getCell(descCol)) : null)
                    .listPrice(price)
                    .currencyCode(currencyCol != null ? emptyToNull(strVal(row.getCell(currencyCol))) : "TRY")
                    .build());
        }
        return rows;
    }

    public List<StockSnapshot> parseStock(Workbook wb, PriceImportTemplate template,
                                          Long batchId, Map<String, Integer> mapping) {
        Sheet sheet = wb.getSheetAt(0);
        List<StockSnapshot> rows = new ArrayList<>();

        Integer stockCol = mapping.get("STOCK_CODE");
        Integer mfgCol = mapping.get("MANUFACTURER_CODE");
        Integer mfgCol2 = mapping.get("MANUFACTURER_CODE_2");
        Integer mfgCol3 = mapping.get("MANUFACTURER_CODE_3");
        Integer barcodeCol = mapping.get("BARCODE");
        Integer descCol = mapping.get("DESCRIPTION");
        Integer groupCol = mapping.get("PRODUCT_GROUP");
        Integer s1 = mapping.get("SALES1"), s2 = mapping.get("SALES2"),
                s3 = mapping.get("SALES3"), s4 = mapping.get("SALES4");
        if (stockCol == null) {
            throw new IllegalArgumentException("Şablonda STOCK_CODE eşlemesi zorunludur.");
        }

        for (int r = template.getHeaderRowIndex() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String code = strVal(row.getCell(stockCol));
            if (code.isBlank()) continue;

            rows.add(StockSnapshot.builder()
                    .batchId(batchId)
                    .stockCode(normalizeCode(code))
                    // Üretici mal kodu hücresinde birden fazla kod boşlukla ayrılmış olabilir
                    // (örn. "10104    111005") — bu yüzden burada TAM normalize edilmez (whitespace
                    // silinmez), bölme işlemi PriceMatchingService'de yapılır.
                    .manufacturerCode(mfgCol != null ? emptyToNull(strVal(row.getCell(mfgCol))) : null)
                    .manufacturerCode2(mfgCol2 != null ? emptyToNull(strVal(row.getCell(mfgCol2))) : null)
                    .manufacturerCode3(mfgCol3 != null ? emptyToNull(strVal(row.getCell(mfgCol3))) : null)
                    .vehicleGroup(groupCol != null ? extractGroupName(strVal(row.getCell(groupCol))) : null)
                    .barcode(barcodeCol != null ? emptyToNull(strVal(row.getCell(barcodeCol))) : null)
                    .description(descCol != null ? strVal(row.getCell(descCol)) : null)
                    .currentSales1(s1 != null ? numVal(row.getCell(s1)) : null)
                    .currentSales2(s2 != null ? numVal(row.getCell(s2)) : null)
                    .currentSales3(s3 != null ? numVal(row.getCell(s3)) : null)
                    .currentSales4(s4 != null ? numVal(row.getCell(s4)) : null)
                    .build());
        }
        return rows;
    }

    /** "0-Ticari Grup" / "1-Binek Grup" gibi CPM "Araç Türü" değerlerinden temiz grup adı çıkarır. */
    static String extractGroupName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim().replaceFirst("^\\d+\\s*-\\s*", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    /** Baseline kod normalizasyonu: boşluk/tab temizle, büyük harfe çevir. Tedarikçiye özel regex ayrıca uygulanır (bkz. PriceMatchingService). */
    public static String normalizeCode(String raw) {
        return raw == null ? "" : raw.replaceAll("\\s+", "").toUpperCase();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String strVal(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> stripTrailingZero(BigDecimal.valueOf(cell.getNumericCellValue()));
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                    ? stripTrailingZero(BigDecimal.valueOf(cell.getNumericCellValue()))
                    : cell.getStringCellValue().trim();
            default -> "";
        };
    }

    private static String stripTrailingZero(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    /** Sayısal değeri önce gerçek NUMERIC hücre tipinden (en güvenilir), yoksa metinden (₺, boşluk, TR ondalık ayracı temizlenerek) okur. */
    private static BigDecimal numVal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(4, RoundingMode.HALF_UP);
        }
        String raw = strVal(cell);
        if (raw.isBlank()) return null;
        String cleaned = raw.replaceAll("[^0-9,.\\-]", "");
        // TR format: binlik nokta, ondalık virgül (örn. 1.383,00) — virgül varsa nokta binlik ayraçtır
        if (cleaned.contains(",")) {
            cleaned = cleaned.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(cleaned).setScale(4, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
