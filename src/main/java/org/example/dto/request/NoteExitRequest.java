package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import org.example.skills.enums.CollectType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NoteExitRequest(

        @NotNull
        Long id,

        CollectType collectType
) {}
