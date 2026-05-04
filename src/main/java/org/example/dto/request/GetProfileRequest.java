package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GetProfileRequest(
        @NotBlank(message = "Token bos olamaz")
        String token
) {
}
