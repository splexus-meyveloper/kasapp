package org.example.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashUpdateRequestDto(

        @NotNull
        @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalı")
        BigDecimal amount,

        @NotBlank
        String description
) {
}