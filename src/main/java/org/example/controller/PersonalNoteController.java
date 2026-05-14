package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.PersonalNoteRequest;
import org.example.dto.response.PersonalNoteResponse;
import org.example.security.CustomUserDetails;
import org.example.service.PersonalNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/my-notes")
@RequiredArgsConstructor
public class PersonalNoteController {

    private final PersonalNoteService service;

    @GetMapping
    public List<PersonalNoteResponse> getNotes(
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.getNotes(user.getId());
    }

    @GetMapping("/{noteId}")
    public PersonalNoteResponse getNote(
            @PathVariable Long noteId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.getNote(noteId, user.getId());
    }

    @PostMapping
    public PersonalNoteResponse createNote(
            @Valid @RequestBody PersonalNoteRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.createNote(request, user.getCompanyId(), user.getId());
    }

    @PutMapping("/{noteId}")
    public PersonalNoteResponse updateNote(
            @PathVariable Long noteId,
            @Valid @RequestBody PersonalNoteRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.updateNote(noteId, request, user.getId());
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long noteId,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.deleteNote(noteId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
