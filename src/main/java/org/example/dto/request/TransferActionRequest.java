package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferActionRequest(

        @NotNull
        Long transferId,

        // Reddedilince gerekçe
        @Size(max = 500)
        String rejectReason
) {}
