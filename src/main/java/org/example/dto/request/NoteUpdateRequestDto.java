package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NoteUpdateRequestDto(
        @NotNull BigDecimal amount,
        String description,
        LocalDate dueDate
) {
}