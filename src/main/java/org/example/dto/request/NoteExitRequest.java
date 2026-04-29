package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NoteExitRequest(

        @NotNull
        Long id,

        String endorsedTo,    // Ciro edilen firma

        String description    // Açıklama
) {}