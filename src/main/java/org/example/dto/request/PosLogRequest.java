package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.skills.enums.PosTerminal;
import org.example.skills.enums.PosType;

import java.math.BigDecimal;

public record PosLogRequest(

        @NotNull(message = "POS tipi seçilmelidir")
        PosType posType,

        @NotNull(message = "Terminal seçilmelidir")
        PosTerminal terminal,

        @NotNull @Positive
        BigDecimal amount,

        @NotBlank(message = "Aciklama bos olamaz")
        @Size(max = 255)
        String description
) {}
