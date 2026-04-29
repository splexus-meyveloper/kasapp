package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.AddExpenseRequest;
import org.example.entity.Expense;
import org.example.repository.ExpenseRepository;
import org.example.skills.enums.AuditAction;
import org.example.skills.enums.ExpenseType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository      expenseRepository;
    private final CashService            cashService;
    private final RealtimeEventService   realtimeEventService;

    @Audit(action = AuditAction.EXPENSE_ADD)
    @Transactional
    public void addExpense(AddExpenseRequest req, Long userId, Long companyId) {

        // Araç gideri ise plaka zorunlu
        if (req.expenseType() == ExpenseType.ARAC_GIDERLERI && req.aracPlaka() == null) {
            throw new IllegalArgumentException("Araç gideri için plaka seçilmelidir.");
        }

        // Açıklamaya plakayı ekle
        String aciklama = req.description();
        if (req.aracPlaka() != null) {
            aciklama = "[" + req.aracPlaka().getLabel() + "] " + aciklama;
        }

        Expense expense = Expense.builder()
                .companyId(companyId)
                .type(req.expenseType())
                .amount(req.amount())
                .description(aciklama)
                .expenseDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .build();

        expenseRepository.save(expense);
        realtimeEventService.publish("MASRAF", "EXPENSE_ADD", companyId, expense.getId());

        cashService.addExpenseFromExpenseModule(
                req.amount(),
                req.expenseType().name() + " - " + aciklama,
                userId,
                companyId
        );
    }
}