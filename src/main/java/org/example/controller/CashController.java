package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CashRequest;
import org.example.security.CustomUserDetails;
import org.example.service.CashService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@CrossOrigin(origins = "http://127.0.0.1:5500")
@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
public class CashController {
    private final CashService service;

    @PostMapping("/income")
    public void income(@Valid @RequestBody CashRequest request,
                       @AuthenticationPrincipal CustomUserDetails user){

        service.addIncome(
                request.amount(),
                request.description(),
                user.getId(),
                user.getCompanyId()
        );
    }

    @PostMapping("/expense")
    public void expense(@Valid @RequestBody CashRequest request,
                        @AuthenticationPrincipal CustomUserDetails user){

        service.addExpense(
                request.amount(),
                request.description(),
                user.getId(),
                user.getCompanyId()
        );
    }

    @GetMapping("/balance")
    public BigDecimal balance(@AuthenticationPrincipal CustomUserDetails user) {
        return service.getBalance(user.getCompanyId());
    }
}
