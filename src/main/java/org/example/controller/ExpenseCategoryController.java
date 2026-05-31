package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.ExpenseCategory;
import org.example.security.CustomUserDetails;
import org.example.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

    private final ExpenseService expenseService;

    /** Tüm giriş yapmış kullanıcılar görebilir — masraf formu için gerekli */
    @GetMapping
    public List<ExpenseCategory> getCategories(
            @AuthenticationPrincipal CustomUserDetails user) {
        return expenseService.getCategories(user.getCompanyId());
    }

    /** Sadece admin yeni masraf türü ekleyebilir */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExpenseCategory> addCategory(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        String code  = body.getOrDefault("code", "").trim();
        String label = body.getOrDefault("label", "").trim();
        if (code.isEmpty() || label.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(expenseService.addCategory(code, label, user.getCompanyId()));
    }

    /** Sadece admin özel masraf türü silebilir */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        expenseService.deleteCategory(id, user.getCompanyId());
        return ResponseEntity.ok().build();
    }
}
