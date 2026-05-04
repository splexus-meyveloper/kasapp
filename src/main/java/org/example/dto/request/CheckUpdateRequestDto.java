package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CheckUpdateRequestDto(
        @NotBlank(message = "Cek numarasi bos olamaz")
        @Size(max = 50)
        String checkNo,

        @NotNull(message = "Tutar bos olamaz")
        @Positive(message = "Tutar 0'dan buyuk olmalidir")
        @Digits(integer = 12, fraction = 2)
        BigDecimal amount,

        @NotBlank(message = "Aciklama bos olamaz")
        @Size(max = 255)
        String description,

        @NotBlank(message = "Banka bos olamaz")
        String bank,

        @NotNull(message = "Vade tarihi bos olamaz")
        @FutureOrPresent(message = "Vade tarihi gecmis olamaz")
        LocalDate dueDate
) {}
