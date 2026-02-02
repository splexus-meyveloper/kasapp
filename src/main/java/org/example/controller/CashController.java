package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.CashRequest;
import org.example.security.CustomUserDetails;
import org.example.service.CashService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
public class CashController {
    private final CashService service;

    @PostMapping("/income")
    public void income(@RequestBody CashRequest request,
                       @AuthenticationPrincipal CustomUserDetails user){

        service.addIncome(
                request.amount(),
                request.description(),
                user.getId(),
                user.getCompanyId()
        );
    }

    @PostMapping("/expense")
    public void expense(@RequestBody CashRequest request,
                        @AuthenticationPrincipal CustomUserDetails user){

        service.addExpense(
                request.amount(),
                request.description(),
                user.getId(),
                user.getCompanyId()
        );
    }

    @GetMapping("/balance")
    public BigDecimal balance(){
        return service.getBalance();
    }
}
