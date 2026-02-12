package org.example.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NoteListResponse(
        String noteNo,
        LocalDate dueDate,
        BigDecimal amount,
        String description
) {
}
