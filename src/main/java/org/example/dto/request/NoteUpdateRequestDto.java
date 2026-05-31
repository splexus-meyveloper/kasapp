package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NoteUpdateRequestDto(
        @NotNull(message = "Tutar bos olamaz")
        @Positive(message = "Tutar 0'dan buyuk olmalidir")
        BigDecimal amount,

        @NotBlank(message = "Aciklama bos olamaz")
        @Size(max = 255)
        String description,

        @NotNull(message = "Vade tarihi bos olamaz")
        @FutureOrPresent(message = "Vade tarihi gecmis olamaz")
        LocalDate dueDate,

        /** Senet numarası — opsiyonel, boş bırakılırsa mevcut numara korunur */
        String noteNo
) {
}
