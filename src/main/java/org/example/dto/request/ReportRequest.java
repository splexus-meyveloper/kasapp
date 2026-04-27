package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReportRequest(

        @NotNull(message = "Başlangıç tarihi boş olamaz")
        LocalDate startDate,

        @NotNull(message = "Bitiş tarihi boş olamaz")
        LocalDate endDate
) {}