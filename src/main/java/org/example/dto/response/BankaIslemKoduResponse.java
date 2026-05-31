package org.example.dto.response;

public record BankaIslemKoduResponse(
        Long id,          // null → built-in enum; non-null → kullanıcı tanımlı (silinebilir)
        String kod,
        String aciklama,
        String direction
) {}
