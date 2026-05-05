package org.example.dto.response;

import org.example.skills.enums.PosType;

import java.util.List;

public record PosTerminalGroupResponse(
        PosType posType,
        List<PosTerminalOption> terminals
) {
}
