package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.audit.AuditDesc;
import org.example.skills.enums.Banka;

import java.time.LocalDate;

public record CheckExitRequest(
        @NotBlank
        String checkNo,

        @NotNull
        Banka bank,

        @NotNull
        LocalDate dueDate,

        @Size(max=255)
        @AuditDesc
        String description
) {}

