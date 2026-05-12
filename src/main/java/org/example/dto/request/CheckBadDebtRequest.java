package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.skills.enums.CheckStatus;

public record CheckBadDebtRequest(
        @NotNull
        Long id,

        // KARSILISIZ veya PROTESTOLU
        @NotNull
        CheckStatus badStatus,

        @Size(max = 500)
        String description
) {}
