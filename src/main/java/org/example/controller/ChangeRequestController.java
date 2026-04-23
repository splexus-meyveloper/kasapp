package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CashUpdateRequestDto;
import org.example.dto.request.CheckUpdateRequestDto;
import org.example.dto.request.NoteUpdateRequestDto;
import org.example.security.CustomUserDetails;
import org.example.service.ChangeRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    // CASH
    @PostMapping("/cash/{cashId}")
    public ResponseEntity<?> requestCashUpdate(
            @PathVariable Long cashId,
            @Valid @RequestBody CashUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        changeRequestService.createCashUpdateRequest(
                cashId,
                dto,
                user.getId(),
                user.getCompanyId()
        );

        return ResponseEntity.ok("Talep başarıyla oluşturuldu");
    }

    // CHECK
    @PostMapping("/check/{checkId}")
    public ResponseEntity<?> requestCheckUpdate(
            @PathVariable Long checkId,
            @Valid @RequestBody CheckUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        changeRequestService.createCheckUpdateRequest(
                checkId,
                dto,
                user.getId(),
                user.getCompanyId()
        );

        return ResponseEntity.ok("Çek düzenleme talebi oluşturuldu");
    }

    // NOTE
    @PostMapping("/note/{noteId}")
    public ResponseEntity<?> requestNoteUpdate(
            @PathVariable Long noteId,
            @Valid @RequestBody NoteUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        changeRequestService.createNoteUpdateRequest(
                noteId,
                dto,
                user.getId(),
                user.getCompanyId()
        );

        return ResponseEntity.ok("Senet düzenleme talebi oluşturuldu");
    }

    // ADMIN → pending
    @GetMapping("/pending")
    public ResponseEntity<?> pending(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(
                changeRequestService.getPendingRequests(user.getCompanyId())
        );
    }

    // APPROVE
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        changeRequestService.approveRequest(
                requestId,
                user.getId(),
                user.getCompanyId()
        );

        return ResponseEntity.ok("Talep onaylandı");
    }

    // REJECT
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        changeRequestService.rejectRequest(
                requestId,
                user.getId()
        );

        return ResponseEntity.ok("Talep reddedildi");
    }
}