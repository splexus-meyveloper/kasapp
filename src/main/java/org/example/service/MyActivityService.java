package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.MyActivityDto;
import org.example.entity.AuditLog;
import org.example.entity.ChangeRequest;
import org.example.repository.AuditLogRepository;
import org.example.repository.ChangeRequestRepository;
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

    public List<MyActivityDto> getMyActivities(Long userId, Long companyId) {

        List<MyActivityDto> result = new ArrayList<>();

        // 🔵 AUDIT LOGS
        List<AuditLog> audits =
                auditRepo.findByCompanyIdAndUserIdOrderByCreatedAtDesc(companyId, userId);

        for (AuditLog log : audits) {

            result.add(MyActivityDto.builder()
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
                    .build());
        }

        // 🟡 CHANGE REQUESTS
        List<ChangeRequest> requests =
                changeRepo.findByCompanyIdAndRequestedByOrderByRequestedAtDesc(companyId, userId);

        for (ChangeRequest req : requests) {

            result.add(MyActivityDto.builder()
                    .source("CHANGE_REQUEST")
                    .action("CASH_UPDATE_REQUEST")
                    .actionLabel("Kasa Düzenleme Talebi")
                    .amount(null)
                    .description("Kasa hareketi düzenleme talebi")
                    .status(req.getStatus() != null ? req.getStatus().name() : "UNKNOWN")
                    .date(req.getRequestedAt() != null ? req.getRequestedAt() : LocalDateTime.now())
                    .direction("NONE")
                    .date(req.getRequestedAt())
                    .entityId(req.getEntityId())
                    .entityType(req.getEntityType())
                    .build());
        }

        // 🔴 SORT
        result.sort(Comparator.comparing(MyActivityDto::getDate).reversed());

        return result;
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
        if (action.contains("INCOME") || action.contains("IN")) return "IN";
        if (action.contains("EXPENSE") || action.contains("OUT")) return "OUT";
        return "NONE";
    }
}