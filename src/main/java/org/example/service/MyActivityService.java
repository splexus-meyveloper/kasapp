package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.MyActivityDto;
import org.example.entity.AuditLog;
import org.example.entity.ChangeRequest;
import org.example.entity.Check;
import org.example.entity.Note;
import org.example.repository.AuditLogRepository;
import org.example.repository.ChangeRequestRepository;
import org.example.repository.CheckRepository;
import org.example.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyActivityService {

    private final AuditLogRepository auditRepo;
    private final ChangeRequestRepository changeRepo;
    private final CheckRepository checkRepository;
    private final NoteRepository noteRepository;

    public List<MyActivityDto> getMyActivities(Long userId, Long companyId) {

        List<MyActivityDto> result = new ArrayList<>();

        List<AuditLog> audits =
                auditRepo.findByCompanyIdAndUserIdOrderByCreatedAtDesc(companyId, userId);

        for (AuditLog log : audits) {

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
                    .entityType(log.getEntityType());

            enrichEntityFields(builder, log.getEntityType(), log.getEntityId(), companyId);

            result.add(builder.build());
        }

        List<ChangeRequest> requests =
                changeRepo.findByCompanyIdAndRequestedByOrderByRequestedAtDesc(companyId, userId);

        for (ChangeRequest req : requests) {

            MyActivityDto.MyActivityDtoBuilder builder = MyActivityDto.builder()
                    .source("CHANGE_REQUEST")
                    .action("CASH_UPDATE_REQUEST")
                    .actionLabel("Kasa Düzenleme Talebi")
                    .amount(null)
                    .description("Kasa hareketi düzenleme talebi")
                    .status(req.getStatus() != null ? req.getStatus().name() : "UNKNOWN")
                    .direction("NONE")
                    .date(req.getRequestedAt() != null ? req.getRequestedAt() : LocalDateTime.now())
                    .entityId(req.getEntityId())
                    .entityType(req.getEntityType());

            enrichEntityFields(builder, req.getEntityType(), req.getEntityId(), companyId);

            result.add(builder.build());
        }

        result.sort(Comparator.comparing(MyActivityDto::getDate).reversed());
        return result;
    }

    private void enrichEntityFields(MyActivityDto.MyActivityDtoBuilder builder,
                                    String entityType,
                                    Long entityId,
                                    Long companyId) {

        if (entityId == null || entityType == null) {
            return;
        }

        if ("CHECK".equalsIgnoreCase(entityType)) {
            Check check = checkRepository.findByIdAndCompanyId(entityId, companyId).orElse(null);

            if (check != null) {
                builder
                        .checkNo(check.getCheckNo())
                        .bank(check.getBank() != null ? check.getBank().name() : null)
                        .dueDate(check.getDueDate());
            }
            return;
        }

        if ("NOTE".equalsIgnoreCase(entityType)) {
            Note note = noteRepository.findByIdAndCompanyId(entityId, companyId).orElse(null);

            if (note != null) {
                builder
                        .noteNo(note.getNoteNo())
                        .dueDate(note.getDueDate())
                        .debtor(null); // NOTE entity'de debtor alanı yok
            }
        }
    }

    private String mapAction(String action) {
        return switch (action) {
            case "CASH_INCOME" -> "Kasa Giriş";
            case "CASH_EXPENSE" -> "Kasa Çıkış";
            case "CHECK_IN" -> "Çek Giriş";
            case "CHECK_COLLECT" -> "Çek Tahsil";
            case "CHECK_OUT" -> "Çek Ödeme";
            case "NOTE_IN" -> "Senet Giriş";
            case "NOTE_COLLECT" -> "Senet Tahsil";
            case "LOAN_CREATE" -> "Kredi Oluşturma";
            case "LOAN_INSTALLMENT" -> "Kredi Taksit";
            case "EXPENSE_ADD" -> "Masraf";
            default -> action;
        };
    }

    private String resolveDirection(String action) {
        if (action == null) return "NONE";
        if (action.contains("INCOME") || action.contains("IN")) return "IN";
        if (action.contains("EXPENSE") || action.contains("OUT")) return "OUT";
        return "NONE";
    }
}