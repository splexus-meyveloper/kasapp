package org.example.dto.response;


import org.example.entity.AuditLog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String username,
        String action,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog l) {
        return new AuditLogResponse(
                l.getId(),
                l.getUsername(),
                l.getAction(),
                l.getAmount(),
                l.getDescription(),
                l.getCreatedAt()
        );
    }
}
