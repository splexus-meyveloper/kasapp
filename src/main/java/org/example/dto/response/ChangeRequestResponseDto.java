package org.example.dto.response;

import org.example.skills.enums.ChangeRequestAction;
import org.example.skills.enums.ChangeRequestStatus;

import java.time.LocalDateTime;

public record ChangeRequestResponseDto(
        Long id,
        String entityType,
        Long entityId,
        ChangeRequestAction actionType,
        String oldData,
        String newData,
        Long requestedBy,
        LocalDateTime requestedAt,
        ChangeRequestStatus status,
        Long approvedBy,
        LocalDateTime approvedAt
) {
}