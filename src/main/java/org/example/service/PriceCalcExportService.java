package org.example.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entity.PriceCalcResult;
import org.example.repository.PriceCalcResultRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * Hesaplanan sonuçları CPM'e yüklenecek Excel'e döker: Mal Kodu / Mal Adı /
 * Alış 1 / Satış 1 / Satış 3 / (Satış 4 — yalnızca kuralda kullanılmışsa).
 * Eşleşmeyen ürünler ayrı bir sekmede listelenir, export'a dahil edilmez.
 */
@Service
@RequiredArgsConstructor
public class PriceCalcExportService {

    private final PriceCalcResultRepository calcResultRepository;

    public byte[] export(Long calcRunId) throws Exception {
        List<PriceCalcResult> all = calcResultRepository.findByCalcRunId(calcRunId);
        List<PriceCalcResult> matched = all.stream().filter(PriceCalcResult::isMatched).toList();
        List<PriceCalcResult> unmatched = all.stream().filter(r -> !r.isMatched()).toList();

        boolean hasSatis4 = matched.stream().anyMatch(r -> r.getNewSales4() != null);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle moneyStyle = createMoneyStyle(wb);

            Sheet sheet = wb.createSheet("Fiyat Güncelleme");
            int row = 0;

            Row header = sheet.createRow(row++);
            String[] headers = hasSatis4
                    ? new String[]{"Mal Kodu", "Mal Adı", "Üretici Mal Kodu", "Alış 1", "Satış 1", "Satış 3", "Satış 4"}
                    : new String[]{"Mal Kodu", "Mal Adı", "Üretici Mal Kodu", "Alış 1", "Satış 1", "Satış 3"};
            for (int c = 0; c < headers.length; c++) {
                writeCell(header, c, headers[c], headerStyle);
            }

            for (PriceCalcResult r : matched) {
                Row dataRow = sheet.createRow(row++);
                writeCell(dataRow, 0, r.getStockCode(), null);
                writeCell(dataRow, 1, r.getProductName(), null);
                writeCell(dataRow, 2, r.getManufacturerCode(), null);
                writeMoneyCell(dataRow, 3, r.getNetAlis(), moneyStyle);
                writeMoneyCell(dataRow, 4, r.getNewSales1(), moneyStyle);
                writeMoneyCell(dataRow, 5, r.getNewSales3(), moneyStyle);
                if (hasSatis4) writeMoneyCell(dataRow, 6, r.getNewSales4(), moneyStyle);
            }

            for (int c = 0; c < headers.length; c++) sheet.autoSizeColumn(c);

            if (!unmatched.isEmpty()) {
                Sheet unmatchedSheet = wb.createSheet("Eşleşmedi - Manuel Kontrol");
                Row uh = unmatchedSheet.createRow(0);
                writeCell(uh, 0, "Mal Kodu", headerStyle);
                writeCell(uh, 1, "Mal Adı", headerStyle);
                writeCell(uh, 2, "Sebep", headerStyle);
                int ur = 1;
                for (PriceCalcResult r : unmatched) {
                    Row dr = unmatchedSheet.createRow(ur++);
                    writeCell(dr, 0, r.getStockCode(), null);
                    writeCell(dr, 1, r.getProductName(), null);
                    writeCell(dr, 2, r.getReason(), null);
                }
                for (int c = 0; c < 3; c++) unmatchedSheet.autoSizeColumn(c);
            }

            wb.write(baos);
            return baos.toByteArray();
        }
    }

    private void writeCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        if (style != null) cell.setCellStyle(style);
    }

    private void writeMoneyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value.doubleValue() : 0);
        if (style != null) cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createMoneyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("#,##0.00 ₺"));
        return style;
    }
}
