package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.skills.enums.Banka;

import java.time.LocalDate;

public record CheckCollectRequest(
        @NotBlank
        String checkNo,

        @NotNull
        Banka bank,

        @NotNull
        LocalDate dueDate
) {}