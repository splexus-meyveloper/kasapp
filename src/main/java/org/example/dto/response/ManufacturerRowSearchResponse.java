package org.example.dto.response;

import java.math.BigDecimal;

public record ManufacturerRowSearchResponse(
        Long id,
        String manufacturerCode,
        String description,
        BigDecimal listPrice
) {}
