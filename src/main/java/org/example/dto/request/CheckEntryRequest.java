package org.example.dto.request;

import jakarta.validation.constraints.*;
import org.example.skills.enums.Banka;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CheckEntryRequest(
        @NotBlank
        @Size(max = 50)
        String checkNo,

        @NotNull
        Banka bank,

        @NotNull
        @FutureOrPresent(message="Vade tarihi geçmiş olamaz")
        LocalDate dueDate,

        @NotNull
        @Positive
        @Digits(integer=12,fraction=2)
        BigDecimal amount,

        @Size(max=255)
        String description
) {}
