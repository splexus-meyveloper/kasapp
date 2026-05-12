package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.*;
import org.example.dto.response.NoteListResponse;
import org.example.entity.Note;
import org.example.security.CustomUserDetails;
import org.example.service.NoteService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> noteIn(
            @Valid @RequestBody NoteEntryRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.noteIn(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Senet giriş yapıldı");
    }

    @PostMapping("/collect")
    public ResponseEntity<?> collect(
            @Valid @RequestBody NoteExitRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.collect(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Senet tahsil edildi");
    }

    @PostMapping("/endorse")
    public ResponseEntity<?> endorse(
            @Valid @RequestBody NoteEndorseRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.endorse(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Senet ciro edildi");
    }

    /** İade: tahsil/cirodan portföye geri al */
    @PostMapping("/return")
    public ResponseEntity<?> returnToPortfolio(
            @Valid @RequestBody CheckReturnRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.returnToPortfolio(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Senet portföye iade edildi");
    }

    /** Protestolu giriş */
    @PostMapping("/protested")
    public ResponseEntity<?> markAsProtested(
            @Valid @RequestBody CheckBadDebtRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.markAsProtested(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Senet protestolu olarak işaretlendi");
    }

    /** Protestoludan çıkış: müşteriye iade veya avukata */
    @PostMapping("/protested/exit")
    public ResponseEntity<?> exitProtested(
            @Valid @RequestBody CheckBadDebtExitRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.exitProtested(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Senet işlemi tamamlandı");
    }

    /** Portföyde olanlar — eski endpoint korunuyor */
    @GetMapping("/portfolio")
    public List<NoteListResponse> portfolio(
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.getPortfolioNotes(user.getCompanyId());
    }

    /** Tüm senetler — frontend filtre yapar */
    @GetMapping("/all")
    public List<NoteListResponse> all(
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.getAllNotes(user.getCompanyId());
    }
}
