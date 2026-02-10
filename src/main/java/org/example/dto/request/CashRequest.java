package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CashRequest(
                          @NotNull
                          @Positive(message = "Tutar 0'dan büyük olmalı")
                          BigDecimal amount,
                          @NotBlank(message = "Açıklama boş olamaz")
                          @Size(max = 255, message = "Açıklama çok uzun")
                          String description) {
}
