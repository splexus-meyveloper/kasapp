package org.example.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.example.skills.enums.ExpensePaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
        Long id,
        String expenseType,
        ExpensePaymentMethod paymentMethod,
        BigDecimal amount,
        String description,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate expenseDate,
        LocalDateTime createdAt,
        Long createdBy
) {}
