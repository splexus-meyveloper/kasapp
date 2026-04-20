package org.example.dto.request;

import jakarta.validation.constraints.*;
import org.example.audit.AuditAmount;
import org.example.audit.AuditDesc;
import org.example.skills.enums.Banka;
import org.example.skills.enums.CheckType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CheckEntryRequest(
        @NotBlank
        @Size(max = 50)
        String checkNo,

        @NotNull
        Banka bank,

        @NotNull
        @FutureOrPresent(message="Vade tarihi geçmiş olamaz")
        LocalDate dueDate,

        @NotNull
        @Positive
        @Digits(integer=12,fraction=2)
        @AuditAmount
        BigDecimal amount,

        @NotBlank
        @Size(max = 255)
        @AuditDesc
        String description,

        @NotNull
        CheckType checkType
) {}
