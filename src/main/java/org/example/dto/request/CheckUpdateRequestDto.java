package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CheckUpdateRequestDto(
        String checkNo,   // ✅ EKLENDİ
        BigDecimal amount,
        String description,
        String bank,
        LocalDate dueDate
) {}