package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.skills.enums.CheckBadDebtExitType;

public record CheckBadDebtExitRequest(
        @NotNull
        Long id,

        // MUSTERI_IADE veya AVUKATA_CIKIS
        @NotNull
        CheckBadDebtExitType exitType,

        @Size(max = 500)
        String description
) {}
