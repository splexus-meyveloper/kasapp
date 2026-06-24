package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.AddExpenseRequest;
import org.example.dto.response.ExpenseResponse;
import org.example.entity.Expense;
import org.example.entity.ExpenseCategory;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.ExpenseCategoryRepository;
import org.example.repository.ExpenseRepository;
import org.example.skills.enums.AuditAction;
import org.example.skills.enums.ExpensePaymentMethod;
import org.example.skills.enums.ExpenseType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final CashService cashService;
    private final RealtimeEventService realtimeEventService;

    // ─────────────────────────────────────────────────────────────
    // KATEGORİ YÖNETİMİ
    // ─────────────────────────────────────────────────────────────

    /** Şirkete ait tüm kategorileri döner (built-in + özel) */
    public List<ExpenseCategory> getCategories(Long companyId) {
        return categoryRepository.findAllForCompany(companyId);
    }

    @Transactional
    public ExpenseCategory addCategory(String code, String label, Long companyId) {
        String normalizedCode = code.trim().toUpperCase().replace(" ", "_");

        // Built-in enum ile çakışma kontrolü
        boolean builtIn = Arrays.stream(ExpenseType.values())
                .anyMatch(e -> e.name().equals(normalizedCode));
        if (builtIn) {
            throw new KasappException(ErrorType.VALIDATION_ERROR);
        }

        if (categoryRepository.existsByCodeAndCompanyId(normalizedCode, companyId)) {
            throw new KasappException(ErrorType.VALIDATION_ERROR);
        }

        ExpenseCategory cat = ExpenseCategory.builder()
                .code(normalizedCode)
                .label(label.trim())
                .companyId(companyId)
                .build();

        return categoryRepository.save(cat);
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long companyId) {
        ExpenseCategory cat = categoryRepository.findByIdAndCompanyId(categoryId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.VALIDATION_ERROR));

        // Built-in kategoriler silinemez (companyId == null)
        if (cat.getCompanyId() == null) {
            throw new KasappException(ErrorType.VALIDATION_ERROR);
        }

        categoryRepository.delete(cat);
    }

    // ─────────────────────────────────────────────────────────────
    // MASRAF EKLEME
    // ─────────────────────────────────────────────────────────────

    @Audit(action = AuditAction.EXPENSE_ADD)
    @Transactional
    public Expense addExpense(AddExpenseRequest req, Long userId, Long companyId) {
        String aciklama = normalizeDescription(req.description());

        ExpensePaymentMethod paymentMethod = req.paymentMethod() == null
                ? ExpensePaymentMethod.CASH
                : req.paymentMethod();

        if ("ARAC_GIDERLERI".equals(req.expenseType())) {
            if (req.aracPlaka() == null) {
                throw new IllegalArgumentException("Arac gideri icin plaka secilmelidir.");
            }
            validateVehicleExpenseDescription(aciklama, req.aracPlaka().getLabel(), req.aracPlaka().name());
        }

        if (req.aracPlaka() != null) {
            aciklama = "[" + req.aracPlaka().getLabel() + "] " + aciklama;
        }

        LocalDate expDate = (req.transactionDate() != null) ? req.transactionDate() : LocalDate.now();
        LocalDateTime createdAt = (req.transactionDate() != null) ? expDate.atStartOfDay() : LocalDateTime.now();

        Expense expense = Expense.builder()
                .companyId(companyId)
                .type(req.expenseType())
                .paymentMethod(paymentMethod)
                .amount(req.amount())
                .description(aciklama)
                .expenseDate(expDate)
                .createdAt(createdAt)
                .createdBy(userId)
                .build();

        expenseRepository.save(expense);
        realtimeEventService.publish("MASRAF", "EXPENSE_ADD", companyId, expense.getId());

        if (paymentMethod == ExpensePaymentMethod.CASH) {
            var cashTx = cashService.addExpenseForMasraf(
                    req.amount(),
                    req.expenseType() + " - " + aciklama,
                    userId,
                    companyId,
                    expDate
            );
            expense.setCashTransactionId(cashTx.getId());
            expenseRepository.save(expense);
        }

        return expense;
    }

    // ─────────────────────────────────────────────────────────────
    // LİSTELEME
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public List<ExpenseResponse> getExpenses(Long companyId,
                                             String expenseType,
                                             ExpensePaymentMethod paymentMethod,
                                             LocalDate startDate,
                                             LocalDate endDate) {
        String typeFilter = (expenseType == null || expenseType.isBlank()) ? null : expenseType;
        return expenseRepository.findFiltered(companyId, typeFilter, paymentMethod, startDate, endDate)
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
}
