package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

public record CheckPaidRequest(
        @NotNull
        Long id,

        String description
) {}