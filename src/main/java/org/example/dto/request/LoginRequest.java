package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull
        String companyCode,
        @NotNull
        String username,
        @NotNull
        String password
) {}