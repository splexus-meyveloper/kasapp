package org.example.dto.request;

import jakarta.validation.constraints.Size;

/** Tüm alanlar opsiyonel — yalnızca gönderilen alan güncellenir. */
public record UpdatePriceSupplierRequest(
        @Size(max = 200)
        String name,
        Boolean active,
        /** Virgülle ayrılmış, üretici kodundan yok sayılacak ön ekler (örn. "US,USA"). */
        @Size(max = 300)
        String ignoredCodePrefixes
) {}
