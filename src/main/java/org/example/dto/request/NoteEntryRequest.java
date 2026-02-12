package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.audit.AuditAmount;
import org.example.audit.AuditDesc;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NoteEntryRequest(
        @NotBlank
        String noteNo,

        @NotNull
        LocalDate dueDate,

        @NotNull
        @Positive
        @AuditAmount
        BigDecimal amount,

        @NotBlank
        @Size(max = 255)
        @AuditDesc String description
) {
}
