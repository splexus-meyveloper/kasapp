package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductGroupRequest(

        @NotBlank(message = "Ürün grubu adı boş olamaz")
        @Size(max = 200)
        String name,

        /** Null ise bu grup tüm tedarikçiler için ortak kullanılabilir. */
        Long supplierId
) {}
