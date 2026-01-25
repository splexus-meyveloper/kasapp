package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        Long adminCompanyId,
        @NotNull
        String username,
        @NotNull
        String password
) {
}
