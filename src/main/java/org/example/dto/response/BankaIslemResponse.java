package org.example.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BankaIslemResponse(
        Long id,
        String aciklama,
        BigDecimal tutar,
        String islemKoduAdi,
        int islemKodu,
        String direction,

        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate islemTarihi
) {}