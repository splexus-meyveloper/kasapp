package org.example.dto.response;

public record BankaIslemKoduResponse(
        String kod,        // int → String oldu
        String aciklama,
        String direction
) {}