package org.example.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.example.skills.enums.PosTerminal;
import org.example.skills.enums.PosType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PosLogResponse(
        Long id,
        PosType posType,
        String posTypeLabel,
        PosTerminal terminal,
        String terminalLabel,
        BigDecimal amount,
        String description,
        String username,

        @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
        LocalDateTime logDate
) {}