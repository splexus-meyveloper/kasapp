package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.audit.AuditDesc;

import java.time.LocalDate;

public record NoteExitRequest(
        @NotBlank String noteNo,
        @NotNull LocalDate dueDate,

        @AuditDesc
        String description
) {
}
