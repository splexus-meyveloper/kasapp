package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.CashTransaction;
import org.example.entity.Check;
import org.example.entity.Expense;
import org.example.entity.Note;
import org.example.repository.CashTransactionRepository;
import org.example.repository.CheckRepository;
import org.example.repository.ExpenseRepository;
import org.example.repository.NoteRepository;
import org.example.security.CustomUserDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global arama — başlık çubuğundaki arama kutusu için.
 * Sayfada yüklü olmayan (eski) kayıtlar da bulunabilsin diye sunucu tarafında arar.
 * Sonuçlar şirket (companyId) kapsamındadır; modül yetkisi olmayan kullanıcıya
 * o modülün sonuçları dönmez.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private static final int LIMIT = 8;

    private final CheckRepository checkRepository;
    private final NoteRepository noteRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final ExpenseRepository expenseRepository;

    @GetMapping
    public Map<String, Object> search(@RequestParam("q") String q,
                                      @AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> result = new LinkedHashMap<>();
        String query = q == null ? "" : q.trim();
        if (query.length() < 2) {
            result.put("checks",   List.of());
            result.put("notes",    List.of());
            result.put("cash",     List.of());
            result.put("expenses", List.of());
            return result;
        }

        Long companyId = user.getCompanyId();
        boolean isAdmin = "ADMIN".equals(user.getRole());
        Pageable limit = PageRequest.of(0, LIMIT);

        result.put("checks", hasModule(user, isAdmin, "CEK")
                ? checkRepository.searchForGlobal(companyId, query, limit).stream().map(this::mapCheck).toList()
                : List.of());

        result.put("notes", hasModule(user, isAdmin, "SENET")
                ? noteRepository.searchForGlobal(companyId, query, limit).stream().map(this::mapNote).toList()
                : List.of());

        // Kasa: admin tüm şirketi, normal kullanıcı yalnızca kendi işlemlerini arar
        List<CashTransaction> cashHits;
        if (isAdmin) {
            cashHits = cashTransactionRepository.searchForGlobal(companyId, query, limit);
        } else if (hasModule(user, false, "KASA")) {
            cashHits = cashTransactionRepository.searchForGlobalByUser(companyId, user.getId(), query, limit);
        } else {
            cashHits = List.of();
        }
        result.put("cash", cashHits.stream().map(this::mapCash).toList());

        // Masraf: admin tüm şirket, normal kullanıcı kendi kayıtları
        List<Expense> expenseHits;
        if (isAdmin) {
            expenseHits = expenseRepository.searchForGlobal(companyId, query, limit);
        } else if (hasModule(user, false, "MASRAF")) {
            expenseHits = expenseRepository.searchForGlobalByUser(companyId, user.getId(), query, limit);
        } else {
            expenseHits = List.of();
        }
        result.put("expenses", expenseHits.stream().map(this::mapExpense).toList());

        return result;
    }

    private boolean hasModule(CustomUserDetails user, boolean isAdmin, String permission) {
        if (isAdmin) return true;
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }

    private Map<String, Object> mapCheck(Check c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          c.getId());
        m.put("checkNo",     c.getCheckNo());
        m.put("bank",        c.getBank() != null ? c.getBank().name() : null);
        m.put("amount",      c.getAmount());
        m.put("dueDate",     c.getDueDate());
        m.put("status",      c.getStatus() != null ? c.getStatus().name() : null);
        m.put("description", c.getDescription());
        return m;
    }

    private Map<String, Object> mapNote(Note n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          n.getId());
        m.put("noteNo",      n.getNoteNo());
        m.put("amount",      n.getAmount());
        m.put("dueDate",     n.getDueDate());
        m.put("status",      n.getStatus() != null ? n.getStatus().name() : null);
        m.put("description", n.getDescription());
        return m;
    }

    private Map<String, Object> mapCash(CashTransaction t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              t.getId());
        m.put("type",            t.getType() != null ? t.getType().name() : null);
        m.put("amount",          t.getAmount());
        m.put("description",     t.getDescription());
        m.put("transactionDate", t.getTransactionDate());
        return m;
    }

    private Map<String, Object> mapExpense(Expense e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          e.getId());
        m.put("type",        e.getType());
        m.put("amount",      e.getAmount());
        m.put("description", e.getDescription());
        m.put("expenseDate", e.getExpenseDate());
        return m;
    }
}
