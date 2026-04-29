package org.example.dto.response;

import org.example.skills.enums.PosTerminal;
import org.example.skills.enums.PosType;

public record PosTerminalInfo(
        PosTerminal value,
        String label,
        PosType posType
) {}