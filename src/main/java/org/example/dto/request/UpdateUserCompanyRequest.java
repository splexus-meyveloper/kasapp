package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserCompanyRequest(
        @NotNull(message = "Şube seçimi zorunludur")
        Long companyId
) {}
