package org.example.dto.response;

import org.example.skills.enums.PosTerminal;

public record PosTerminalOption(
        PosTerminal value,
        String label
) {
}
