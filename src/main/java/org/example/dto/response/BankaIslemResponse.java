package org.example.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BankaIslemResponse(
        Long id,
        String aciklama,
        BigDecimal tutar,
        String islemKoduAdi,
        String islemKodu,    // int → String oldu
        String direction,
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate islemTarihi
) {}