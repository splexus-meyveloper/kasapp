package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.AddExpenseRequest;
import org.example.security.CustomUserDetails;
import org.example.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<?> addExpense(
            @Valid @RequestBody AddExpenseRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {

        expenseService.addExpense(req, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Masraf eklendi");
    }
}