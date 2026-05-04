package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.NoteEndorseRequest;
import org.example.dto.request.NoteEntryRequest;
import org.example.dto.request.NoteExitRequest;
import org.example.dto.response.NoteListResponse;
import org.example.entity.Note;
import org.example.security.CustomUserDetails;
import org.example.service.NoteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SENET') or hasRole('ADMIN')")
public class NoteController {

    private final NoteService service;

    @PostMapping("/in")
    public void noteIn(
            @Valid @RequestBody NoteEntryRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.noteIn(req, user.getId(), user.getCompanyId());
    }

    @PostMapping("/collect")
    public Note collect(
            @Valid @RequestBody NoteExitRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.collect(req, user.getId(), user.getCompanyId());
    }

    @PostMapping("/endorse")
    public Note endorse(
            @Valid @RequestBody NoteEndorseRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.endorse(req, user.getId(), user.getCompanyId());
    }

    @GetMapping("/portfolio")
    public List<NoteListResponse> portfolio(
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.getPortfolioNotes(user.getCompanyId());
    }
}
