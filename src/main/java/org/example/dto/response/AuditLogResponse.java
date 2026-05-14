package org.example.dto.response;

import org.example.audit.AuditDetails;
import org.example.audit.AuditDirectionResolver;
import org.example.entity.AuditLog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogResponse(
        Long id,
        String username,
        String action,
        BigDecimal amount,
        String direction,
        String description,
        LocalDateTime createdAt,
        String expenseType,
        String paymentMethod,
        AuditDetails detailsJson
) {
    public static AuditLogResponse from(AuditLog log) {
        Map<String, Object> payload = log.getDetailsJson() != null ? log.getDetailsJson().getPayload() : null;

        return new AuditLogResponse(
                log.getId(),
                log.getUsername(),
                log.getAction(),
                log.getAmount(),
                AuditDirectionResolver.resolve(log.getAction(), log.getCashDirection()),
                log.getDescription(),
                log.getCreatedAt(),
                stringValue(payload, "expenseType"),
                stringValue(payload, "paymentMethod"),
                log.getDetailsJson()
        );
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }
}
