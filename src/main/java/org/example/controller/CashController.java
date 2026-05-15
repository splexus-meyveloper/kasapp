package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CashRequest;
import org.example.dto.response.PageResponse;
import org.example.entity.CashTransaction;
import org.example.security.CustomUserDetails;
import org.example.service.CashService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.example.skills.enums.ERole;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('KASA') or hasRole('ADMIN')")
public class CashController {

    private final CashService service;

    @PostMapping("/income")
    public void income(@Valid @RequestBody CashRequest request,
                       @AuthenticationPrincipal CustomUserDetails user) {
        service.addIncome(request.amount(), request.description(),
                user.getId(), user.getCompanyId());
    }

    @PostMapping("/expense")
    public void expense(@Valid @RequestBody CashRequest request,
                        @AuthenticationPrincipal CustomUserDetails user) {
        service.addExpense(request.amount(), request.description(),
                user.getId(), user.getCompanyId());
    }

    @GetMapping("/balance")
    public BigDecimal balance(@AuthenticationPrincipal CustomUserDetails user) {
        return service.getBalance(user.getCompanyId());
    }

    // ✅ Pagination parametreleri eklendi
    @GetMapping("/transactions")
    public PageResponse<CashTransaction> getTransactions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        return service.getTransactions(
                user.getId(),
                user.getCompanyId(),
                ERole.valueOf(user.getRole()),
                page,
                size
        );
    }

    /** Kasadan bankaya para çıkışı — sadece admin, kasa bakiyesinden düşer */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bank-withdrawal")
    public void bankWithdrawal(
            @Valid @RequestBody CashRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.addExpense(
                request.amount(),
                "[BANKAYA YATIRMA] " + request.description(),
                user.getId(),
                user.getCompanyId()
        );
    }
}
