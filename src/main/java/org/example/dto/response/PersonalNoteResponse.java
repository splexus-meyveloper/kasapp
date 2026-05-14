package org.example.dto.response;

import org.example.entity.PersonalNote;

import java.time.LocalDateTime;

public record PersonalNoteResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PersonalNoteResponse from(PersonalNote note) {
        return new PersonalNoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
