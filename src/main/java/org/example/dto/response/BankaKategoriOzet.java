package org.example.dto.response;

import java.math.BigDecimal;

/** Kategori bazında banka hareketi özeti (rapor satırı). */
public record BankaKategoriOzet(
        String kategoriAdi,
        String kod,
        BigDecimal giris,
        BigDecimal cikis,
        BigDecimal net,
        long adet
) {
}
