package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.audit.AuditAmount;
import org.example.audit.AuditDesc;
import org.example.dto.response.PageResponse;
import org.example.entity.CashTransaction;
import org.example.repository.CashTransactionRepository;
import org.example.skills.enums.AuditAction;
import org.example.skills.enums.CashDirection;
import org.example.skills.enums.ERole;
import org.example.skills.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CashService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE     = 200;

    private final CashTransactionRepository repository;
    private final RealtimeEventService realtimeEventService;

    @Audit(action = AuditAction.CASH_INCOME, cash = CashDirection.IN)
    @Transactional
    public CashTransaction addIncome(@AuditAmount BigDecimal amount,
                                     @AuditDesc String description,
                                     Long userId, Long companyId) {
        return addIncome(amount, description, userId, companyId, null);
    }

    @Audit(action = AuditAction.CASH_INCOME, cash = CashDirection.IN)
    @Transactional
    public CashTransaction addIncome(@AuditAmount BigDecimal amount,
                                     @AuditDesc String description,
                                     Long userId, Long companyId, LocalDate overrideDate) {
        CashTransaction tx = createTransaction(TransactionType.INCOME, amount, description, userId, companyId, false, overrideDate);
        realtimeEventService.publish("KASA", "CASH_INCOME", companyId, tx.getId());
        return tx;
    }

    @Audit(action = AuditAction.CASH_EXPENSE, cash = CashDirection.OUT)
    @Transactional
    public CashTransaction addExpense(@AuditAmount BigDecimal amount,
                                      @AuditDesc String description,
                                      Long userId, Long companyId) {
        return addExpense(amount, description, userId, companyId, null);
    }

    @Audit(action = AuditAction.CASH_EXPENSE, cash = CashDirection.OUT)
    @Transactional
    public CashTransaction addExpense(@AuditAmount BigDecimal amount,
                                      @AuditDesc String description,
                                      Long userId, Long companyId, LocalDate overrideDate) {
        CashTransaction tx = createTransaction(TransactionType.EXPENSE, amount, description, userId, companyId, false, overrideDate);
        realtimeEventService.publish("KASA", "CASH_EXPENSE", companyId, tx.getId());
        return tx;
    }

    /**
     * Masrafın kasa karşılığı. Kasa hareketi (ve dolayısıyla bakiye) oluşturur ama
     * AYRI bir CASH_EXPENSE audit kaydı/satırı ÜRETMEZ — masraf zaten EXPENSE_ADD
     * olarak loglanıyor. Böylece "benim işlemlerim"/rapor/log'da çift satır olmaz.
     */
    @Transactional
    public CashTransaction addExpenseForMasraf(BigDecimal amount, String description,
                                               Long userId, Long companyId) {
        return addExpenseForMasraf(amount, description, userId, companyId, null);
    }

    @Transactional
    public CashTransaction addExpenseForMasraf(BigDecimal amount, String description,
                                               Long userId, Long companyId, LocalDate overrideDate) {
        CashTransaction tx = createTransaction(TransactionType.EXPENSE, amount, description, userId, companyId, false, overrideDate);
        realtimeEventService.publish("KASA", "CASH_EXPENSE", companyId, tx.getId());
        return tx;
    }

    @Transactional
    public CashTransaction addTransferIncome(BigDecimal amount, String description, Long userId, Long companyId) {
        CashTransaction tx = createTransaction(TransactionType.INCOME, amount, description, userId, companyId, true, null);
        realtimeEventService.publish("KASA", "TRANSFER_INCOME", companyId, tx.getId());
        return tx;
    }

    @Transactional
    public CashTransaction addTransferExpense(BigDecimal amount, String description, Long userId, Long companyId) {
        CashTransaction tx = createTransaction(TransactionType.EXPENSE, amount, description, userId, companyId, true, null);
        realtimeEventService.publish("KASA", "TRANSFER_EXPENSE", companyId, tx.getId());
        return tx;
    }

    public BigDecimal getBalance(Long companyId) {
        return repository.getCurrentBalance(companyId);
    }

    // ✅ Pagination ile — artık tüm tabloyu çekmez
    public PageResponse<CashTransaction> getTransactions(Long userId, Long companyId,
                                                         ERole role, int page, int size) {
        int safeSize = Math.min(size > 0 ? size : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        PageRequest pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("transactionDate")));

        Page<CashTransaction> result = (role == ERole.ADMIN)
                ? repository.findByCompanyIdAndActiveTrueOrderByTransactionDateDesc(companyId, pageable)
                : repository.findByCompanyIdAndUserIdAndActiveTrueOrderByTransactionDateDesc(companyId, userId, pageable);

        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private CashTransaction createTransaction(TransactionType type, BigDecimal amount,
                                              String description, Long userId, Long companyId,
                                              boolean transferTransaction, LocalDate overrideDate) {
        validate(amount);
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        LocalDateTime txDate = (overrideDate != null)
                ? overrideDate.atStartOfDay()
                : LocalDateTime.now();

        CashTransaction tx = CashTransaction.builder()
                .type(type)
                .amount(amount)
                .description(description)
                .transactionDate(txDate)
                .userId(userId)
                .companyId(companyId)
                .active(true)
                .transferTransaction(transferTransaction)
                .build();

        return repository.save(tx);
    }

    private void validate(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Amount must be positive");
    }
}
