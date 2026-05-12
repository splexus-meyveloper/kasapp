package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.TransferActionRequest;
import org.example.dto.request.TransferCreateRequest;
import org.example.dto.response.TransferResponse;
import org.example.security.CustomUserDetails;
import org.example.service.InterBranchTransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class InterBranchTransferController {

    private final InterBranchTransferService service;

    /** Adapazarı şubesi transfer oluşturur */
    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @Valid @RequestBody TransferCreateRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                service.createTransfer(req, user.getId(), user.getCompanyId()));
    }

    /** Admin: bekleyen transferleri onayla */
    @PostMapping("/approve")
    public ResponseEntity<TransferResponse> approve(
            @Valid @RequestBody TransferActionRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                service.approve(req, user.getId(), user.getCompanyId()));
    }

    /** Admin: reddet */
    @PostMapping("/reject")
    public ResponseEntity<TransferResponse> reject(
            @Valid @RequestBody TransferActionRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                service.reject(req, user.getId()));
    }

    /** Kendi şubemin transferleri */
    @GetMapping("/my")
    public List<TransferResponse> myTransfers(
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.getMyTransfers(user.getCompanyId());
    }

    /** Admin: bekleyen transferler */
    @GetMapping("/pending")
    public List<TransferResponse> pending() {
        return service.getPendingTransfers();
    }

    /** Admin: tüm transferler */
    @GetMapping("/all")
    public List<TransferResponse> all() {
        return service.getAllTransfers();
    }
}
