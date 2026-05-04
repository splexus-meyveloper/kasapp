package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetPermissionsRequest(
        @NotNull(message = "Yetki listesi bos olamaz")
        List<String> permissions
) { }
