package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

        String description
) {}