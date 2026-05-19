package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.*;
import org.example.dto.response.CheckListResponse;
import org.example.dto.response.PageResponse;
import org.example.security.CustomUserDetails;
import org.example.service.CheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checks")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CEK') or hasRole('ADMIN')")
public class CheckController {

    private final CheckService service;

    @PostMapping("/in")
    public ResponseEntity<?> checkIn(
            @Valid @RequestBody CheckEntryRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.checkIn(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Çek giriş yapıldı");
    }

    @PostMapping("/collect")
    public ResponseEntity<?> collect(
            @Valid @RequestBody CheckCollectRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.collect(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Çek tahsil edildi");
    }

    @PostMapping("/endorse")
    public ResponseEntity<?> endorse(
            @Valid @RequestBody CheckEndorseRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.endorse(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Çek ciro edildi");
    }

    /** İade: tahsil/cirodan portföye geri al */
    @PostMapping("/return")
    public ResponseEntity<?> returnToPortfolio(
            @Valid @RequestBody CheckReturnRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.returnToPortfolio(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Çek portföye iade edildi");
    }

    /** Karşılıksız / Protestolu giriş */
    @PostMapping("/bad-debt")
    public ResponseEntity<?> markAsBadDebt(
            @Valid @RequestBody CheckBadDebtRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.markAsBadDebt(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Çek sorunlu olarak işaretlendi");
    }

    /** Karşılıksız/protestoludan çıkış: müşteriye iade veya avukata */
    @PostMapping("/bad-debt/exit")
    public ResponseEntity<?> exitBadDebt(
            @Valid @RequestBody CheckBadDebtExitRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.exitBadDebt(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Çek işlemi tamamlandı");
    }

    @PostMapping("/paid")
    public ResponseEntity<?> markAsPaid(
            @Valid @RequestBody CheckPaidRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.markAsPaid(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok().build();
    }

    /** Portföyde bekleyenler (eski endpoint — korunuyor) */
    @GetMapping("/portfolio")
    public List<CheckListResponse> portfolio(
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.getPortfolioChecks(user.getCompanyId());
    }

    /** Tüm çekler — sayfalama destekli. Merkez admin tüm şubeleri görür. */
    @GetMapping("/all")
    public PageResponse<CheckListResponse> all(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.getAllChecks(user.getCompanyId(), user.getRole(), page, size);
    }
}
