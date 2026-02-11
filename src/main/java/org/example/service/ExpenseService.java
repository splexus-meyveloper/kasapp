package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.AddExpenseRequest;
import org.example.entity.Expense;
import org.example.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CashService cashService;

    @Audit(action = "EXPENSE_ADD")
    @Transactional
    public void addExpense(AddExpenseRequest req,
                           Long userId,
                           Long companyId) {

        Expense expense = Expense.builder()
                .companyId(companyId)
                .type(req.expenseType())
                .amount(req.amount())
                .description(req.description())
                .expenseDate(LocalDate.now()) // ✅ otomatik tarih
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .build();

        expenseRepository.save(expense);

        // kasadan düş
        cashService.addExpenseFromExpenseModule(
                req.amount(),
                req.expenseType().name() + " - " + req.description(),
                userId,
                companyId
        );
    }
}
