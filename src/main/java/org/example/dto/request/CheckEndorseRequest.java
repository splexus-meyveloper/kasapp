package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.audit.AuditDesc;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CheckEndorseRequest(
        @NotNull
        Long id,

        @Size(max = 255)
        String endorsedTo,

        @Size(max = 255)
        @AuditDesc
        String description
) {}
