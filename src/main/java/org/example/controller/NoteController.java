package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.NoteEntryRequest;
import org.example.dto.request.NoteExitRequest;
import org.example.dto.response.NoteListResponse;
import org.example.security.CustomUserDetails;
import org.example.service.NoteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService service;

    @PostMapping("/in")
    public void noteIn(
            @Valid @RequestBody NoteEntryRequest req,
            @AuthenticationPrincipal CustomUserDetails user){

        service.noteIn(req,user.getId(),user.getCompanyId());
    }

    @PostMapping("/out")
    public void noteOut(
            @Valid @RequestBody NoteExitRequest req,
            @AuthenticationPrincipal CustomUserDetails user){

        service.noteOut(req,user.getId(),user.getCompanyId());
    }

    @GetMapping("/portfolio")
    public List<NoteListResponse> portfolio(){

        CustomUserDetails user =
                (CustomUserDetails) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return service.getPortfolioNotes(
                user.getCompanyId()
        );
    }
}

