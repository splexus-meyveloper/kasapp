package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.skills.enums.CollectType;

public record CheckCollectRequest(
        @NotNull
        Long id,
        CollectType collectType
) {}
