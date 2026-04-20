package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.AddExpenseRequest;
import org.example.entity.Expense;
import org.example.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.example.skills.enums.AuditAction;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CashService cashService;
    private final RealtimeEventService realtimeEventService;

    @Audit(action = AuditAction.EXPENSE_ADD)
    @Transactional
    public void addExpense(AddExpenseRequest req,
                           Long userId,
                           Long companyId) {

        Expense expense = Expense.builder()
                .companyId(companyId)
                .type(req.expenseType())
                .amount(req.amount())
                .description(req.description())
                .expenseDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .build();

        expenseRepository.save(expense);
        realtimeEventService.publish("MASRAF", "EXPENSE_ADD", companyId, expense.getId());

        // kasadan düş
        cashService.addExpenseFromExpenseModule(
                req.amount(),
                req.expenseType().name() + " - " + req.description(),
                userId,
                companyId
        );
    }

}
