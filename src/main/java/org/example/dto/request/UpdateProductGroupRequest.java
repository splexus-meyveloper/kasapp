package org.example.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProductGroupRequest(
        @Size(max = 200)
        String name,
        Boolean active
) {}
