package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.example.skills.enums.ERole;

public record AdminCreateUserRequest(
        @NotBlank
        String username,

        @NotBlank
        String password,

        String name,
        String surname,
        String email,
        String phone,

        ERole role
) {
}