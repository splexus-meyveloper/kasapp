package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckPaidRequest(
        @NotNull
        Long id,

        @NotBlank(message = "Aciklama bos olamaz")
        @Size(max = 255)
        String description
) {}
