package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.AddExpenseRequest;
import org.example.dto.response.ExpenseResponse;
import org.example.security.CustomUserDetails;
import org.example.service.ExpenseService;
import org.example.skills.enums.ExpensePaymentMethod;
import org.example.skills.enums.ExpenseType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @RequestParam(required = false) ExpenseType expenseType,
            @RequestParam(required = false) ExpensePaymentMethod paymentMethod,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                expenseService.getExpenses(
                        user.getCompanyId(),
                        expenseType,
                        paymentMethod,
                        startDate,
                        endDate
                )
        );
    }
}
