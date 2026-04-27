package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CalendarNoteRequest;
import org.example.dto.response.CalendarNoteResponse;
import org.example.security.CustomUserDetails;
import org.example.service.CalendarNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendar-notes")
@RequiredArgsConstructor
public class CalendarNoteController {

    private final CalendarNoteService calendarNoteService;

    // Notları getir
    @GetMapping
    public List<CalendarNoteResponse> getNotes(
            @AuthenticationPrincipal CustomUserDetails user) {
        return calendarNoteService.getNotes(user.getCompanyId(), user.getId());
    }

    // Not ekle
    @PostMapping
    public CalendarNoteResponse addNote(
            @Valid @RequestBody CalendarNoteRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return calendarNoteService.addNote(request, user.getCompanyId(), user.getId());
    }

    // Not sil
    @DeleteMapping("/{noteId}")
    public ResponseEntity<?> deleteNote(
            @PathVariable Long noteId,
            @AuthenticationPrincipal CustomUserDetails user) {
        calendarNoteService.deleteNote(noteId, user.getCompanyId(), user.getId());
        return ResponseEntity.ok("Not silindi");
    }
}