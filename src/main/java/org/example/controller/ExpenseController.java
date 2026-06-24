package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.AddExpenseRequest;
import org.example.dto.response.ExpenseResponse;
import org.example.security.CustomUserDetails;
import org.example.service.ExpenseService;
import org.example.skills.enums.ExpensePaymentMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASRAF') or hasRole('ADMIN')")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<?> addExpense(
            @Valid @RequestBody AddExpenseRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        // GECMIS_TARIH yoksa transactionDate yoksayılır — servis null alırsa bugünü kullanır
        AddExpenseRequest effectiveReq = hasGecmisTarih(user) ? req
                : new AddExpenseRequest(req.expenseType(), req.paymentMethod(), req.amount(),
                        req.description(), req.aracPlaka(), null);
        expenseService.addExpense(effectiveReq, user.getId(), user.getCompanyId());
        return ResponseEntity.ok("Masraf eklendi");
    }

    private boolean hasGecmisTarih(CustomUserDetails user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> "GECMIS_TARIH".equals(a.getAuthority()));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @RequestParam(required = false) String expenseType,
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
