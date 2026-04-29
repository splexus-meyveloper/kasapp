package org.example.dto.response;

public record BankaIslemKoduResponse(
        int kod,
        String aciklama,
        String direction
) {}