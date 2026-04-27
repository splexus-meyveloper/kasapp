package org.example.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dto.request.ReportRequest;
import org.example.dto.response.ReportSummaryResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final ReportService reportService;

    public byte[] exportReport(Long companyId, ReportRequest req) throws Exception {

        ReportSummaryResponse report = reportService.getReport(companyId, req);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // Stiller
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle moneyStyle  = createMoneyStyle(wb);
            CellStyle titleStyle  = createTitleStyle(wb);

            // ── Sayfa 1: Genel Özet ──────────────────────────────────
            Sheet summary = wb.createSheet("Genel Özet");
            int row = 0;

            row = writeTitle(summary, row, "GENEL FİNANS ÖZETİ", titleStyle);
            row = writeTitle(summary, row,
                    req.startDate() + " — " + req.endDate(), null);
            row++;

            row = writeKV(summary, row, "Toplam Gelir",
                    report.totalIncome(), moneyStyle);
            row = writeKV(summary, row, "Toplam Gider",
                    report.totalExpense(), moneyStyle);
            row = writeKV(summary, row, "Net Bakiye",
                    report.netBalance(), moneyStyle);
            row = writeKV(summary, row, "Kasa Bakiyesi",
                    report.currentCashBalance(), moneyStyle);
            row++;
            row = writeKV(summary, row, "Çek Portföyü",
                    report.checkPortfolioTotal(), moneyStyle);
            row = writeKV(summary, row, "Senet Portföyü",
                    report.notePortfolioTotal(), moneyStyle);
            row = writeKV(summary, row, "Toplam Kredi Borcu",
                    report.totalLoanDebt(), moneyStyle);

            summary.autoSizeColumn(0);
            summary.autoSizeColumn(1);

            // ── Sayfa 2: Aylık Dağılım ────────────────────────────────
            Sheet monthly = wb.createSheet("Aylık Dağılım");
            row = 0;
            Row hdr = monthly.createRow(row++);
            writeCell(hdr, 0, "Ay", headerStyle);
            writeCell(hdr, 1, "Gelir", headerStyle);
            writeCell(hdr, 2, "Gider", headerStyle);
            writeCell(hdr, 3, "Net", headerStyle);

            for (ReportSummaryResponse.MonthlyBreakdown mb : report.monthlyBreakdown()) {
                Row r = monthly.createRow(row++);
                writeCell(r, 0, mb.month(), null);
                writeMoneyCell(r, 1, mb.income(), moneyStyle);
                writeMoneyCell(r, 2, mb.expense(), moneyStyle);
                writeMoneyCell(r, 3, mb.net(), moneyStyle);
            }
            monthly.autoSizeColumn(0);
            monthly.autoSizeColumn(1);
            monthly.autoSizeColumn(2);
            monthly.autoSizeColumn(3);

            // ── Sayfa 3: Masraf Kategorileri ─────────────────────────
            Sheet expenses = wb.createSheet("Masraf Kategorileri");
            row = 0;
            Row expHdr = expenses.createRow(row++);
            writeCell(expHdr, 0, "Kategori", headerStyle);
            writeCell(expHdr, 1, "Toplam", headerStyle);

            for (Map.Entry<String, BigDecimal> entry
                    : report.expenseByCategory().entrySet()) {
                Row r = expenses.createRow(row++);
                writeCell(r, 0, entry.getKey(), null);
                writeMoneyCell(r, 1, entry.getValue(), moneyStyle);
            }
            expenses.autoSizeColumn(0);
            expenses.autoSizeColumn(1);

            // ── Sayfa 4: Vadesi Yaklaşanlar ───────────────────────────
            Sheet due = wb.createSheet("Vadesi Yaklaşanlar");
            row = 0;

            // Çekler
            Row checkTitle = due.createRow(row++);
            writeCell(checkTitle, 0, "VADESİ YAKLAŞAN ÇEKLER (30 gün)", headerStyle);
            due.addMergedRegion(new CellRangeAddress(row-1, row-1, 0, 3));

            Row checkHdr = due.createRow(row++);
            writeCell(checkHdr, 0, "Çek No", headerStyle);
            writeCell(checkHdr, 1, "Tutar", headerStyle);
            writeCell(checkHdr, 2, "Vade Tarihi", headerStyle);
            writeCell(checkHdr, 3, "Kalan Gün", headerStyle);

            for (ReportSummaryResponse.DueItemResponse c : report.upcomingChecks()) {
                Row r = due.createRow(row++);
                writeCell(r, 0, c.no(), null);
                writeMoneyCell(r, 1, c.amount(), moneyStyle);
                writeCell(r, 2, c.dueDate(), null);
                writeCell(r, 3, String.valueOf(c.daysLeft()), null);
            }

            row++;

            // Senetler
            Row noteTitle = due.createRow(row++);
            writeCell(noteTitle, 0, "VADESİ YAKLAŞAN SENETLER (30 gün)", headerStyle);
            due.addMergedRegion(new CellRangeAddress(row-1, row-1, 0, 3));

            Row noteHdr = due.createRow(row++);
            writeCell(noteHdr, 0, "Senet No", headerStyle);
            writeCell(noteHdr, 1, "Tutar", headerStyle);
            writeCell(noteHdr, 2, "Vade Tarihi", headerStyle);
            writeCell(noteHdr, 3, "Kalan Gün", headerStyle);

            for (ReportSummaryResponse.DueItemResponse n : report.upcomingNotes()) {
                Row r = due.createRow(row++);
                writeCell(r, 0, n.no(), null);
                writeMoneyCell(r, 1, n.amount(), moneyStyle);
                writeCell(r, 2, n.dueDate(), null);
                writeCell(r, 3, String.valueOf(n.daysLeft()), null);
            }

            due.autoSizeColumn(0);
            due.autoSizeColumn(1);
            due.autoSizeColumn(2);
            due.autoSizeColumn(3);

            // ── Sayfa 5: Krediler ──────────────────────────────────────
            Sheet loans = wb.createSheet("Krediler");
            row = 0;
            Row loanHdr = loans.createRow(row++);
            writeCell(loanHdr, 0, "Banka", headerStyle);
            writeCell(loanHdr, 1, "Aylık Taksit", headerStyle);
            writeCell(loanHdr, 2, "Kalan Borç", headerStyle);
            writeCell(loanHdr, 3, "Kalan Taksit", headerStyle);
            writeCell(loanHdr, 4, "Sonraki Ödeme", headerStyle);

            for (ReportSummaryResponse.LoanSummaryItem l : report.activeLoans()) {
                Row r = loans.createRow(row++);
                writeCell(r, 0, l.bankName(), null);
                writeMoneyCell(r, 1, l.monthlyPayment(), moneyStyle);
                writeMoneyCell(r, 2, l.remainingDebt(), moneyStyle);
                writeCell(r, 3, String.valueOf(l.remainingInstallments()), null);
                writeCell(r, 4, l.nextPaymentDate(), null);
            }
            loans.autoSizeColumn(0);
            loans.autoSizeColumn(1);
            loans.autoSizeColumn(2);
            loans.autoSizeColumn(3);
            loans.autoSizeColumn(4);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Yardımcılar ────────────────────────────────────────────────────

    private int writeTitle(Sheet sheet, int rowNum, String text, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        if (style != null) cell.setCellStyle(style);
        return rowNum + 1;
    }

    private int writeKV(Sheet sheet, int rowNum, String key,
                        BigDecimal value, CellStyle moneyStyle) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(key);
        Cell valCell = row.createCell(1);
        valCell.setCellValue(value != null ? value.doubleValue() : 0);
        if (moneyStyle != null) valCell.setCellStyle(moneyStyle);
        return rowNum + 1;
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

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }
}