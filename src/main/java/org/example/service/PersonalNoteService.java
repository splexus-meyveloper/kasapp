package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.PersonalNoteRequest;
import org.example.dto.response.PersonalNoteResponse;
import org.example.entity.PersonalNote;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.PersonalNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalNoteService {

    private final PersonalNoteRepository repository;

    @Transactional(readOnly = true)
    public List<PersonalNoteResponse> getNotes(Long userId) {
        return repository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(PersonalNoteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonalNoteResponse getNote(Long noteId, Long userId) {
        return PersonalNoteResponse.from(getOwnedNote(noteId, userId));
    }

    @Transactional
    public PersonalNoteResponse createNote(PersonalNoteRequest request, Long companyId, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        PersonalNote note = PersonalNote.builder()
                .companyId(companyId)
                .userId(userId)
                .title(request.title().trim())
                .content(request.content())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return PersonalNoteResponse.from(repository.save(note));
    }

    @Transactional
    public PersonalNoteResponse updateNote(Long noteId, PersonalNoteRequest request, Long userId) {
        PersonalNote note = getOwnedNote(noteId, userId);

        note.setTitle(request.title().trim());
        note.setContent(request.content());
        note.setUpdatedAt(LocalDateTime.now());

        return PersonalNoteResponse.from(repository.save(note));
    }

    @Transactional
    public void deleteNote(Long noteId, Long userId) {
        PersonalNote note = getOwnedNote(noteId, userId);
        repository.delete(note);
    }

    private PersonalNote getOwnedNote(Long noteId, Long userId) {
        return repository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new KasappException(ErrorType.PERSONAL_NOTE_NOT_FOUND));
    }
}
