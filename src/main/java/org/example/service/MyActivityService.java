package org.example.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.MyActivityDto;
import org.example.entity.AuditLog;
import org.example.entity.ChangeRequest;
import org.example.entity.Check;
import org.example.entity.Note;
import org.example.entity.PosLog;
import org.example.repository.AuditLogRepository;
import org.example.repository.ChangeRequestRepository;
import org.example.repository.CheckRepository;
import org.example.repository.NoteRepository;
import org.example.repository.PosLogRepository;
import org.example.skills.enums.PosType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MyActivityService {

    private static final int MAX_RECORDS = 200;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final AuditLogRepository      auditRepo;
    private final ChangeRequestRepository changeRepo;
    private final CheckRepository         checkRepository;
    private final NoteRepository          noteRepository;
    private final PosLogRepository        posLogRepository;

    // ── Liste (filtreli) ──────────────────────────────────────────
    public List<MyActivityDto> getMyActivities(Long userId, Long companyId,
                                               String action,
                                               LocalDate startDate, LocalDate endDate,
                                               int page, int size) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end   = endDate   != null ? endDate.plusDays(1).atStartOfDay() : null;

        int safeSize = Math.min(size > 0 ? size : MAX_RECORDS, MAX_RECORDS);

        List<AuditLog> audits = auditRepo
                .findFiltered(companyId, userId, action, start, end,
                        PageRequest.of(page, safeSize))
                .getContent();

        List<MyActivityDto> result = new ArrayList<>();

        for (AuditLog log : audits) {
            if ("POS_LOG".equals(log.getAction())) {
                continue;
            }

            MyActivityDto.MyActivityDtoBuilder builder = MyActivityDto.builder()
                    .source("AUDIT")
                    .action(log.getAction())
                    .actionLabel(mapAction(log.getAction()))
                    .amount(log.getAmount())
                    .description(log.getDescription())
                    .status("COMPLETED")
                    .direction(resolveDirection(log.getAction()))
                    .date(log.getCreatedAt())
                    .entityId(log.getEntityId())
                    .entityType(log.getEntityType())
                    .detailsJson(log.getDetailsJson())
                    .expenseType(getPayloadValue(log, "expenseType"))
                    .paymentMethod(getPayloadValue(log, "paymentMethod"));

            enrichEntityFields(builder, log.getEntityType(), log.getEntityId(), companyId);
            result.add(builder.build());
        }

        if (action == null || "POS_LOG".equals(action)) {
            findMyPosLogs(companyId, userId, start, end, PageRequest.of(page, safeSize))
                    .stream()
                    .map(this::toPosActivity)
                    .forEach(result::add);
        }

        // Filtre yoksa change request'leri de ekle
        if (action == null && startDate == null && endDate == null) {
            List<ChangeRequest> requests = changeRepo
                    .findByCompanyIdAndRequestedByOrderByRequestedAtDesc(
                            companyId, userId, PageRequest.of(0, 50))
                    .getContent();

            for (ChangeRequest req : requests) {
                result.add(MyActivityDto.builder()
                        .source("CHANGE_REQUEST")
                        .action("CASH_UPDATE_REQUEST")
                        .actionLabel("Kasa Düzenleme Talebi")
                        .status(req.getStatus() != null ? req.getStatus().name() : "UNKNOWN")
                        .direction("NONE")
                        .date(req.getRequestedAt() != null ? req.getRequestedAt() : LocalDateTime.now())
                        .entityId(req.getEntityId())
                        .entityType(req.getEntityType())
                        .build());
            }
        }

        result.sort(Comparator.comparing(MyActivityDto::getDate).reversed());
        return result;
    }

    // ── PDF export ────────────────────────────────────────────────
    public byte[] exportPdf(Long userId, Long companyId, String username,
                            String action, LocalDate startDate, LocalDate endDate) {

        List<MyActivityDto> items = getMyActivities(
                userId, companyId, action, startDate, endDate, 0, MAX_RECORDS);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter   writer = new PdfWriter(baos);
            PdfDocument pdf    = new PdfDocument(writer);
            Document    doc    = new Document(pdf);

            PdfFont font = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

            // Başlık
            doc.add(new Paragraph("İşlem Raporu")
                    .setFont(bold).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("Kullanıcı: " + username)
                    .setFont(font).setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

            String donem = "";
            if (startDate != null) donem += "Başlangıç: " + startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "  ";
            if (endDate   != null) donem += "Bitiş: "     + endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            if (!donem.isBlank()) {
                doc.add(new Paragraph(donem).setFont(font).setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER));
            }

            doc.add(new Paragraph(" "));

            // Özet
            BigDecimal toplamGiris  = items.stream()
                    .filter(i -> "IN".equals(i.getDirection()) && i.getAmount() != null)
                    .map(MyActivityDto::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal toplamCikis  = items.stream()
                    .filter(i -> "OUT".equals(i.getDirection()) && i.getAmount() != null)
                    .map(MyActivityDto::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal net = toplamGiris.subtract(toplamCikis);

            Table ozet = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                    .setWidth(UnitValue.createPercentValue(100));

            ozet.addHeaderCell(hCell("Toplam Giriş", bold));
            ozet.addHeaderCell(hCell("Toplam Çıkış", bold));
            ozet.addHeaderCell(hCell("Net", bold));
            ozet.addCell(cell("₺ " + toplamGiris.toPlainString(), font));
            ozet.addCell(cell("₺ " + toplamCikis.toPlainString(), font));
            ozet.addCell(cell("₺ " + net.toPlainString(), font));

            doc.add(ozet);
            doc.add(new Paragraph(" "));

            // İşlem tablosu
            Table tablo = new Table(UnitValue.createPercentArray(new float[]{2, 3, 2, 4}))
                    .setWidth(UnitValue.createPercentValue(100));

            tablo.addHeaderCell(hCell("Tarih", bold));
            tablo.addHeaderCell(hCell("İşlem", bold));
            tablo.addHeaderCell(hCell("Tutar", bold));
            tablo.addHeaderCell(hCell("Açıklama", bold));

            for (MyActivityDto item : items) {
                tablo.addCell(cell(item.getDate() != null ? item.getDate().format(FMT) : "-", font));
                tablo.addCell(cell(item.getActionLabel(), font));
                tablo.addCell(cell(item.getAmount() != null ? "₺ " + item.getAmount().toPlainString() : "-", font));
                tablo.addCell(cell(item.getDescription() != null ? item.getDescription() : "-", font));
            }

            doc.add(tablo);
            doc.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF oluşturulamadı: " + e.getMessage(), e);
        }
    }

    // ── Yardımcı metodlar ────────────────────────────────────────
    private Cell hCell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(9))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell cell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(8));
    }

    private void enrichEntityFields(MyActivityDto.MyActivityDtoBuilder builder,
                                    String entityType, Long entityId, Long companyId) {
        if (entityId == null || entityType == null) return;

        if ("CHECK".equalsIgnoreCase(entityType)) {
            checkRepository.findByIdAndCompanyId(entityId, companyId).ifPresent(c ->
                    builder.checkNo(c.getCheckNo())
                            .bank(c.getBank() != null ? c.getBank().name() : null)
                            .dueDate(c.getDueDate()));
            return;
        }

        if ("NOTE".equalsIgnoreCase(entityType)) {
            noteRepository.findByIdAndCompanyId(entityId, companyId).ifPresent(n ->
                    builder.noteNo(n.getNoteNo())
                            .dueDate(n.getDueDate()));
            return;
        }

        if ("POS".equalsIgnoreCase(entityType)) {
            posLogRepository.findByIdAndCompanyId(entityId, companyId).ifPresent(pos ->
                    builder.amount(pos.getAmount())
                            .description(pos.getDescription())
                            .date(pos.getLogDate())
                            .posType(pos.getPosType() != null ? pos.getPosType().name() : null)
                            .posTypeLabel(posTypeLabel(pos.getPosType()))
                            .terminal(pos.getTerminal() != null ? pos.getTerminal().name() : null)
                            .terminalLabel(pos.getTerminal() != null ? pos.getTerminal().getLabel() : null)
                            .paymentMethod("CREDIT_CARD"));
        }
    }

    private String mapAction(String action) {
        return switch (action) {
            case "CASH_INCOME"      -> "Kasa Giriş";
            case "CASH_EXPENSE"     -> "Kasa Çıkış";
            case "CHECK_IN"         -> "Çek Giriş";
            case "CHECK_COLLECT"    -> "Çek Tahsil";
            case "CHECK_OUT"        -> "Çek Ödeme";
            case "NOTE_IN"          -> "Senet Giriş";
            case "NOTE_COLLECT"     -> "Senet Tahsil";
            case "LOAN_CREATE"      -> "Kredi Oluşturma";
            case "LOAN_INSTALLMENT" -> "Kredi Taksit";
            case "EXPENSE_ADD"      -> "Masraf";
            case "POS_LOG"          -> "POS İşlemi";
            case "BANKA_CIKIS"      -> "Bankaya Para Yatırma";
            default                 -> action;
        };
    }

    private MyActivityDto toPosActivity(PosLog pos) {
        return MyActivityDto.builder()
                .source("POS_LOG")
                .action("POS_LOG")
                .actionLabel("POS Islemi")
                .amount(pos.getAmount())
                .description(pos.getDescription())
                .status("COMPLETED")
                .direction("NONE")
                .date(pos.getLogDate())
                .entityId(pos.getId())
                .entityType("POS")
                .posType(pos.getPosType() != null ? pos.getPosType().name() : null)
                .posTypeLabel(posTypeLabel(pos.getPosType()))
                .terminal(pos.getTerminal() != null ? pos.getTerminal().name() : null)
                .terminalLabel(pos.getTerminal() != null ? pos.getTerminal().getLabel() : null)
                .paymentMethod("CREDIT_CARD")
                .build();
    }

    private List<PosLog> findMyPosLogs(Long companyId, Long userId,
                                       LocalDateTime start, LocalDateTime end,
                                       PageRequest pageable) {
        if (start != null && end != null) {
            return posLogRepository.findByCompanyIdAndUserIdAndLogDateGreaterThanEqualAndLogDateLessThanOrderByLogDateDesc(
                    companyId, userId, start, end, pageable);
        }
        if (start != null) {
            return posLogRepository.findByCompanyIdAndUserIdAndLogDateGreaterThanEqualOrderByLogDateDesc(
                    companyId, userId, start, pageable);
        }
        if (end != null) {
            return posLogRepository.findByCompanyIdAndUserIdAndLogDateLessThanOrderByLogDateDesc(
                    companyId, userId, end, pageable);
        }
        return posLogRepository.findByCompanyIdAndUserIdOrderByLogDateDesc(
                companyId, userId, pageable);
    }

    private String posTypeLabel(PosType posType) {
        if (posType == null) return null;
        return switch (posType) {
            case ALTIKARDESLER_POS -> "Altikardesler POS";
            case TEDARIKCI_POS -> "Tedarikci POS";
            case YAZARKASA_POS -> "Yazarkasa POS";
        };
    }

    private String resolveDirection(String action) {
        if (action == null) return "NONE";
        if (action.contains("INCOME") || action.contains("IN")) return "IN";
        if (action.contains("EXPENSE") || action.contains("OUT")) return "OUT";
        return "NONE";
    }

    private String getPayloadValue(AuditLog log, String key) {
        if (log.getDetailsJson() == null || log.getDetailsJson().getPayload() == null) {
            return null;
        }
        Object value = log.getDetailsJson().getPayload().get(key);
        return value != null ? value.toString() : null;
    }
}
