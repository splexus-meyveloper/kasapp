package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.TransferActionRequest;
import org.example.dto.request.TransferCreateRequest;
import org.example.dto.response.TransferResponse;
import org.example.security.CustomUserDetails;
import org.example.service.InterBranchTransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class InterBranchTransferController {

    private final InterBranchTransferService service;

    /**
     * Transfer türüne göre gerekli modül yetkisi — önceden bu uç noktanın hiç
     * @PreAuthorize'ı yoktu, yani hiçbir modül yetkisi olmayan bir kullanıcı bile
     * şubeden merkeze nakit/banka/çek-senet transfer talebi oluşturabiliyordu.
     */
    private void requireTransferPermission(TransferCreateRequest req, CustomUserDetails user) {
        String required = switch (req.transferType()) {
            case NAKIT_GONDERIM -> "KASA";
            case BANKA_YATIRMA -> "BANKA";
            case CEK_SENET -> (req.checkIds() != null && !req.checkIds().isEmpty()) ? "CEK" : "SENET";
        };
        boolean allowed = user.getAuthorities().stream().anyMatch(a ->
                required.equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
        if (!allowed) {
            throw new AccessDeniedException("Bu transfer türü için " + required + " yetkisi gereklidir.");
        }
    }

    /** Adapazarı şubesi transfer oluşturur */
    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @Valid @RequestBody TransferCreateRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        requireTransferPermission(req, user);
        return ResponseEntity.ok(
                service.createTransfer(req, user.getId(), user.getCompanyId()));
    }

    /** Admin: bekleyen transferleri onayla */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/approve")
    public ResponseEntity<TransferResponse> approve(
            @Valid @RequestBody TransferActionRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                service.approve(req, user.getId(), user.getCompanyId()));
    }

    /** Admin: reddet */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reject")
    public ResponseEntity<TransferResponse> reject(
            @Valid @RequestBody TransferActionRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                service.reject(req, user.getId(), user.getCompanyId()));
    }

    /** Tek transfer detayı — log sayfasındaki Detay butonu için */
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(service.getById(id, user.getCompanyId()));
    }

    /** Kendi şubemin transferleri */
    @GetMapping("/my")
    public List<TransferResponse> myTransfers(
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.getMyTransfers(user.getCompanyId());
    }

    /** Admin: bekleyen transferler */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public List<TransferResponse> pending() {
        return service.getPendingTransfers();
    }

    /** Admin: tüm transferler */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public List<TransferResponse> all() {
        return service.getAllTransfers();
    }
}
