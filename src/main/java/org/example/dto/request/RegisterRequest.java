package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotNull
        String companyName,
        @NotNull
        String name,
        @NotNull
        String surname,
        @NotNull
        String email,
        @NotNull
        String phone,
        @NotNull
        String username,
        @NotNull
        String password
) {

}
