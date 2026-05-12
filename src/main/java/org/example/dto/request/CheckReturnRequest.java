package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckReturnRequest(
        @NotNull
        Long id,

        @Size(max = 500)
        String description
) {}
