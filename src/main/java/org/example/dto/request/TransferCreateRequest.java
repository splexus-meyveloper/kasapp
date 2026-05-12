package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.skills.enums.TransferType;

import java.math.BigDecimal;
import java.util.List;

public record TransferCreateRequest(

        @NotNull
        TransferType transferType,

        // Nakit / banka transferlerinde zorunlu
        @Positive
        BigDecimal amount,

        @Size(max = 500)
        String description,

        // CEK_SENET transferinde: seçilen çek id'leri
        List<Long> checkIds,

        // CEK_SENET transferinde: seçilen senet id'leri
        List<Long> noteIds
) {}
