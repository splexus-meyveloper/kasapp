package org.example.dto.response;

import org.example.entity.CalendarNote;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CalendarNoteResponse(
        Long id,
        LocalDate date,
        String text,
        LocalDateTime createdAt
) {
    public static CalendarNoteResponse from(CalendarNote note) {
        return new CalendarNoteResponse(
                note.getId(),
                note.getDate(),
                note.getText(),
                note.getCreatedAt()
        );
    }
}