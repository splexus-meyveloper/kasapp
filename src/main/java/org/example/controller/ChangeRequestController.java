package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CashUpdateRequestDto;
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

    // 🔵 USER → talep oluştur
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

    // 🔵 ADMIN → pending talepler
    @GetMapping("/pending")
    public ResponseEntity<?> pending(
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        return ResponseEntity.ok(
                changeRequestService.getPendingRequests(user.getCompanyId())
        );
    }

    // 🟢 ADMIN → onay
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

    // 🔴 ADMIN → red
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