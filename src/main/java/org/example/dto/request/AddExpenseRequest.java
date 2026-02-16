package org.example.dto.request;

import jakarta.validation.constraints.*;
import org.example.audit.AuditAmount;
import org.example.audit.AuditDesc;
import org.example.skills.enums.ExpenseType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddExpenseRequest(
        @NotNull(message = "Masraf türü seçilmelidir")
        ExpenseType expenseType,

        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 2)
        @AuditAmount
        BigDecimal amount,

        @NotBlank
        @Size(max = 255)
        @AuditDesc
        String description
) {
}
