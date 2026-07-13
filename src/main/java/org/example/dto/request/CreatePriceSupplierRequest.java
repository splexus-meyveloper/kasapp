package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePriceSupplierRequest(

        @NotBlank(message = "Tedarikçi kodu boş olamaz")
        @Size(max = 100)
        String code,

        @NotBlank(message = "Tedarikçi adı boş olamaz")
        @Size(max = 200)
        String name
) {}
