package org.example.dto.response;

import org.example.skills.enums.NoteStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record NoteListResponse(
        Long id,
        String noteNo,
        String debtor,
        LocalDate dueDate,
        BigDecimal amount,
        String description,
        NoteStatus status,
        LocalDateTime createdAt,
        Long companyId
) {}
