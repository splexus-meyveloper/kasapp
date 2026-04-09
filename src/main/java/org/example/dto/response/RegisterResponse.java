package org.example.dto.response;

public record RegisterResponse(
        String message,
        String companyCode,
        String username
) {}