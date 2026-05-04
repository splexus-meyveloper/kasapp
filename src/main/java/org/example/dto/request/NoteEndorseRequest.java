package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NoteEndorseRequest(
        @NotNull
        Long id,

        @NotBlank(message = "Ciro edilen kisi/firma bos olamaz")
        @Size(max = 255)
        String endorsedTo,

        @NotBlank(message = "Aciklama bos olamaz")
        @Size(max = 255)
        String description
) {}
