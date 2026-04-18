package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.audit.AuditDesc;

public record CheckEndorseRequest(
        @NotNull
        Long id,

        @Size(max = 255)
        @AuditDesc
        String description
) {}