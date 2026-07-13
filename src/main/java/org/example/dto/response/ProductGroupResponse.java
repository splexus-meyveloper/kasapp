package org.example.dto.response;

public record ProductGroupResponse(
        Long id,
        String name,
        Long supplierId,
        boolean active
) {}
