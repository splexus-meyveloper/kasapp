package org.example.dto.response;

public record PriceSupplierResponse(
        Long id,
        String code,
        String name,
        boolean active,
        String ignoredCodePrefixes
) {}
