package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

public record CheckCollectRequest(
        @NotNull
        Long id
) {}