package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.AddExpenseRequest;
import org.example.dto.response.ExpenseResponse;
import org.example.entity.Expense;
import org.example.repository.ExpenseRepository;
import org.example.skills.enums.AuditAction;
import org.example.skills.enums.ExpensePaymentMethod;
import org.example.skills.enums.ExpenseType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CashService cashService;
    private final RealtimeEventService realtimeEventService;

    @Audit(action = AuditAction.EXPENSE_ADD)
    @Transactional
    public Expense addExpense(AddExpenseRequest req, Long userId, Long companyId) {
        String aciklama = normalizeDescription(req.description());

        ExpensePaymentMethod paymentMethod = req.paymentMethod() == null
                ? ExpensePaymentMethod.CASH
                : req.paymentMethod();

        if (req.expenseType() == ExpenseType.ARAC_GIDERLERI) {
            if (req.aracPlaka() == null) {
                throw new IllegalArgumentException("Arac gideri icin plaka secilmelidir.");
            }
            validateVehicleExpenseDescription(aciklama, req.aracPlaka().getLabel(), req.aracPlaka().name());
        }

        if (req.aracPlaka() != null) {
            aciklama = "[" + req.aracPlaka().getLabel() + "] " + aciklama;
        }

        Expense expense = Expense.builder()
                .companyId(companyId)
                .type(req.expenseType())
                .paymentMethod(paymentMethod)
                .amount(req.amount())
                .description(aciklama)
                .expenseDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .build();

        expenseRepository.save(expense);
        realtimeEventService.publish("MASRAF", "EXPENSE_ADD", companyId, expense.getId());

        if (paymentMethod == ExpensePaymentMethod.CASH) {
            cashService.addExpenseFromExpenseModule(
                    req.amount(),
                    req.expenseType().name() + " - " + aciklama,
                    userId,
                    companyId
            );
        }

        return expense;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Aciklama bos olamaz.");
        }

        String normalized = description.trim();
        if (normalized.equalsIgnoreCase("null") || normalized.equalsIgnoreCase("undefined")) {
            throw new IllegalArgumentException("Aciklama bos olamaz.");
        }

        return normalized;
    }

    private void validateVehicleExpenseDescription(String description, String plateLabel, String plateCode) {
        String normalized = description.replaceAll("\\s+", " ").trim();
        String normalizedPlateLabel = plateLabel.replaceAll("\\s+", " ").trim();
        String normalizedPlateCode = plateCode.replace('_', ' ').replaceFirst("^P ", "").trim();

        if (normalized.equalsIgnoreCase(normalizedPlateLabel)
                || normalized.equalsIgnoreCase(normalizedPlateCode)
                || normalized.equalsIgnoreCase("arac gideri")
                || normalized.equalsIgnoreCase("araç gideri")) {
            throw new IllegalArgumentException("Arac gideri icin aciklama girilmelidir.");
        }
    }

    @Transactional
    public List<ExpenseResponse> getExpenses(Long companyId,
                                             ExpenseType expenseType,
                                             ExpensePaymentMethod paymentMethod,
                                             LocalDate startDate,
                                             LocalDate endDate) {
        return expenseRepository.findFiltered(companyId, expenseType, paymentMethod, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getType(),
                expense.getPaymentMethod() == null ? ExpensePaymentMethod.CASH : expense.getPaymentMethod(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getCreatedAt(),
                expense.getCreatedBy()
        );
    }
}
