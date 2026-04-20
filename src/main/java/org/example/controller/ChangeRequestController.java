package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CashUpdateRequestDto;
import org.example.security.CustomUserDetails;
import org.example.service.ChangeRequestService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    // USER -> kasa hareketi düzenleme talebi gönderir
    @PostMapping("/cash/{cashId}")
    public void requestCashUpdate(@PathVariable Long cashId,
                                  @Valid @RequestBody CashUpdateRequestDto dto,
                                  @AuthenticationPrincipal CustomUserDetails user) throws Exception {

        changeRequestService.createCashUpdateRequest(
                cashId,
                dto,
                user.getId(),
                user.getCompanyId()
        );
    }

    // ADMIN -> pending talepleri listeler
    @GetMapping("/pending")
    public Object getPendingRequests() {
        return changeRequestService.getPendingRequests();
    }

    // ADMIN -> onaylar
    @PostMapping("/{requestId}/approve")
    public void approveRequest(@PathVariable Long requestId,
                               @AuthenticationPrincipal CustomUserDetails user) throws Exception {

        changeRequestService.approveRequest(
                requestId,
                user.getId(),
                user.getCompanyId()
        );
    }

    // ADMIN -> reddeder
    @PostMapping("/{requestId}/reject")
    public void rejectRequest(@PathVariable Long requestId,
                              @AuthenticationPrincipal CustomUserDetails user) {

        changeRequestService.rejectRequest(
                requestId,
                user.getId()
        );
    }
}