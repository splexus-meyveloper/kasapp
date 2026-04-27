package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CalendarNoteRequest(

        @NotNull(message = "Tarih boş olamaz")
        LocalDate date,

        @NotBlank(message = "Not içeriği boş olamaz")
        @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
        String text
) {}