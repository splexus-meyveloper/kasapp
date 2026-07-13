package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CashUpdateRequestDto;
import org.example.dto.request.CheckUpdateRequestDto;
import org.example.dto.request.ExpenseUpdateRequestDto;
import org.example.dto.request.NoteUpdateRequestDto;
import org.example.dto.request.PosUpdateRequestDto;
import org.example.security.CustomUserDetails;
import org.example.service.ChangeRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    /** Kullanıcının DOGRUDAN_ISLEM yetkisi var mı? */
    private boolean isDirect(CustomUserDetails user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> "DOGRUDAN_ISLEM".equals(a.getAuthority()));
    }

    /**
     * Hangi kayıt türünün hangi modül yetkisini gerektirdiği — bu uç noktalar önceden
     * hiç @PreAuthorize taşımıyordu, yani MASRAF yetkisi olan biri (hatta hiç modül
     * yetkisi olmayan biri) bir ÇEK ya da SENET kaydı için düzenleme/silme talebi
     * oluşturabiliyordu. Talep DOGRUDAN_ISLEM ile aynı anda onaylanıp uygulandığından
     * bu sadece bir "talep oluşturma" formalitesi değil, gerçek bir yetki açığıydı.
     */
    private static final Map<String, String> ENTITY_PERMISSION = Map.of(
            "CASH", "KASA",
            "CHECK", "CEK",
            "NOTE", "SENET",
            "POS", "KASA",
            "EXPENSE", "MASRAF",
            "TRANSFER", "KASA",
            "BANKA_HESAP", "BANKA",
            "BANKA_ISLEM", "BANKA"
    );

    private void requirePermission(CustomUserDetails user, String entityType) {
        String required = ENTITY_PERMISSION.get(entityType);
        if (required == null) return; // bilinmeyen tip — servis katmanı zaten reddedecek
        boolean allowed = user.getAuthorities().stream().anyMatch(a ->
                required.equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
        if (!allowed) {
            throw new AccessDeniedException("Bu işlem için " + required + " yetkisi gereklidir.");
        }
    }

    @PostMapping("/cash/{cashId}")
    public ResponseEntity<?> requestCashUpdate(
            @PathVariable Long cashId,
            @Valid @RequestBody CashUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        requirePermission(user, "CASH");
        Long requestId = changeRequestService.createCashUpdateRequest(
                cashId, dto, user.getId(), user.getCompanyId());

        if (isDirect(user)) {
            changeRequestService.approveRequest(requestId, user.getId(), user.getCompanyId());
            return ResponseEntity.ok("Kasa hareketi güncellendi");
        }
        return ResponseEntity.ok("Talep basariyla olusturuldu");
    }

    @PostMapping("/check/{checkId}")
    public ResponseEntity<?> requestCheckUpdate(
            @PathVariable Long checkId,
            @Valid @RequestBody CheckUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        requirePermission(user, "CHECK");
        Long requestId = changeRequestService.createCheckUpdateRequest(
                checkId, dto, user.getId(), user.getCompanyId());

        if (isDirect(user)) {
            changeRequestService.approveRequest(requestId, user.getId(), user.getCompanyId());
            return ResponseEntity.ok("Çek güncellendi");
        }
        return ResponseEntity.ok("Cek duzenleme talebi olusturuldu");
    }

    @PostMapping("/note/{noteId}")
    public ResponseEntity<?> requestNoteUpdate(
            @PathVariable Long noteId,
            @Valid @RequestBody NoteUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        requirePermission(user, "NOTE");
        Long requestId = changeRequestService.createNoteUpdateRequest(
                noteId, dto, user.getId(), user.getCompanyId());

        if (isDirect(user)) {
            changeRequestService.approveRequest(requestId, user.getId(), user.getCompanyId());
            return ResponseEntity.ok("Senet güncellendi");
        }
        return ResponseEntity.ok("Senet duzenleme talebi olusturuldu");
    }

    @PostMapping("/pos/{posLogId}")
    public ResponseEntity<?> requestPosUpdate(
            @PathVariable Long posLogId,
            @Valid @RequestBody PosUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        requirePermission(user, "POS");
        Long requestId = changeRequestService.createPosUpdateRequest(
                posLogId, dto, user.getId(), user.getCompanyId());

        if (isDirect(user)) {
            changeRequestService.approveRequest(requestId, user.getId(), user.getCompanyId());
            return ResponseEntity.ok("POS kaydı güncellendi");
        }
        return ResponseEntity.ok("POS duzenleme talebi olusturuldu");
    }

    @PostMapping("/expense/{expenseId}")
    public ResponseEntity<?> requestExpenseUpdate(
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpenseUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        requirePermission(user, "EXPENSE");
        Long requestId = changeRequestService.createExpenseUpdateRequest(
                expenseId, dto, user.getId(), user.getCompanyId());

        if (isDirect(user)) {
            changeRequestService.approveRequest(requestId, user.getId(), user.getCompanyId());
            return ResponseEntity.ok("Masraf güncellendi");
        }
        return ResponseEntity.ok("Masraf duzenleme talebi olusturuldu");
    }

    /** İşlem SİLME — DOGRUDAN_ISLEM yetkisi varsa anında silinir, yoksa onaya gider */
    @PostMapping("/delete/{entityType}/{entityId}")
    public ResponseEntity<?> requestDelete(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        requirePermission(user, entityType == null ? "" : entityType.trim().toUpperCase());
        Long requestId = changeRequestService.createDeleteRequest(
                entityType, entityId, user.getId(), user.getCompanyId());

        if (isDirect(user)) {
            changeRequestService.approveRequest(requestId, user.getId(), user.getCompanyId());
            return ResponseEntity.ok("İşlem silindi");
        }
        return ResponseEntity.ok("Silme talebi olusturuldu");
    }

    /** Tek change request detayı */
    @GetMapping("/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getById(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(changeRequestService.getById(requestId, user.getCompanyId()));
    }

    /** Tüm change requestler — PENDING + APPROVED + REJECTED */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAll(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(changeRequestService.getAllRequests(user.getCompanyId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> pending(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(
                changeRequestService.getPendingRequests(user.getCompanyId())
        );
    }

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        changeRequestService.approveRequest(requestId, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Talep onaylandi");
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        changeRequestService.rejectRequest(requestId, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Talep reddedildi");
    }
}
