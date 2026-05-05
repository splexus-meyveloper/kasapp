package org.example.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.audit.AuditDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MyActivityDto {

    private String source;
    private String action;
    private String actionLabel;

    private BigDecimal amount;
    private String description;

    private String status;
    private String direction;

    private LocalDateTime date;

    private Long entityId;
    private String entityType;

    private String expenseType;
    private String paymentMethod;
    private AuditDetails detailsJson;

    private String checkNo;
    private String bank;
    private LocalDate dueDate;

    private String noteNo;
    private String debtor;

    private String posType;
    private String posTypeLabel;
    private String terminal;
    private String terminalLabel;
}
