package org.example.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankaHesapResponse(
        Long id,
        String hesapKodu,
        String bankaAdi,
        String hesapNumarasi,
        BigDecimal baslangicBakiye,
        BigDecimal guncelBakiye,
        LocalDateTime olusturmaTarihi
) {}