package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.CalendarNoteRequest;
import org.example.dto.response.CalendarNoteResponse;
import org.example.entity.CalendarNote;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.CalendarNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarNoteService {

    private final CalendarNoteRepository repository;

    // Kullanıcının tüm takvim notlarını getir
    @Transactional(readOnly = true)
    public List<CalendarNoteResponse> getNotes(Long companyId, Long userId) {
        return repository
                .findByCompanyIdAndUserIdOrderByDateAsc(companyId, userId)
                .stream()
                .map(CalendarNoteResponse::from)
                .toList();
    }

    // Yeni not ekle
    @Transactional
    public CalendarNoteResponse addNote(CalendarNoteRequest request,
                                        Long companyId, Long userId) {
        CalendarNote note = CalendarNote.builder()
                .companyId(companyId)
                .userId(userId)
                .date(request.date())
                .text(request.text())
                .createdAt(LocalDateTime.now())
                .build();

        return CalendarNoteResponse.from(repository.save(note));
    }

    // Not sil — sadece sahibi silebilir
    @Transactional
    public void deleteNote(Long noteId, Long companyId, Long userId) {
        CalendarNote note = repository
                .findByIdAndCompanyIdAndUserId(noteId, companyId, userId)
                .orElseThrow(() -> new KasappException(ErrorType.CHANGE_REQUEST_NOT_FOUND));

        repository.delete(note);
    }
}