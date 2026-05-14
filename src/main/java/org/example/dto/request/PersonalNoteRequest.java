package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonalNoteRequest(

        @NotBlank(message = "Başlık boş olamaz")
        @Size(max = 120, message = "Başlık en fazla 120 karakter olabilir")
        String title,

        @NotBlank(message = "Not içeriği boş olamaz")
        @Size(max = 20000, message = "Not içeriği en fazla 20000 karakter olabilir")
        String content
) {}
