package org.example.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MyActivityDto {

    private String source; // AUDIT / CHANGE_REQUEST
    private String action;
    private String actionLabel;

    private BigDecimal amount;
    private String description;

    private String status; // COMPLETED / PENDING / APPROVED / REJECTED
    private String direction; // IN / OUT / NONE

    private LocalDateTime date;

    private Long entityId;
    private String entityType;

    // CHECK prefill
    private String checkNo;
    private String bank;
    private LocalDate dueDate;

    // NOTE prefill
    private String noteNo;
    private String debtor;
}