package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

public record NoteExitRequest(
        @NotNull
        Long id
) {}