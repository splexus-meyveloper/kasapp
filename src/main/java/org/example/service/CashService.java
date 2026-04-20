package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.entity.CashTransaction;
import org.example.entity.User;
import org.example.repository.CashTransactionRepository;
import org.example.skills.enums.CashDirection;
import org.example.skills.enums.ERole;
import org.example.skills.enums.TransactionType;
import org.springframework.stereotype.Service;
import org.example.audit.AuditDesc;
import org.example.audit.AuditAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.example.skills.enums.AuditAction;

@Service
@RequiredArgsConstructor
public class CashService {

    private final CashTransactionRepository repository;
    private final RealtimeEventService realtimeEventService;


    @Audit(
            action = AuditAction.CASH_INCOME,
            cash = CashDirection.IN
    )
    @Transactional
    public CashTransaction addIncome(

            @AuditAmount BigDecimal amount,
            @AuditDesc String description,
            Long userId,
            Long companyId) {

        CashTransaction tx = createTransaction(
                TransactionType.INCOME,
                amount,
                description,
                userId,
                companyId
        );

        realtimeEventService.publish("KASA", "CASH_INCOME", companyId, tx.getId());

        return tx;
    }


    @Audit(
            action = AuditAction.CASH_EXPENSE,
            cash = CashDirection.OUT
    )
    @Transactional
    public CashTransaction addExpense(

            @AuditAmount BigDecimal amount,
            @AuditDesc String description,
            Long userId,
            Long companyId) {

        CashTransaction tx = createTransaction(
                TransactionType.EXPENSE,
                amount,
                description,
                userId,
                companyId
        );

        realtimeEventService.publish("KASA", "CASH_EXPENSE", companyId, tx.getId());

        return tx;
    }


    @Transactional
    public CashTransaction addExpenseFromExpenseModule(

            @AuditAmount BigDecimal amount,
            @AuditDesc String description,
            Long userId,
            Long companyId) {

        CashTransaction tx = createTransaction(
                TransactionType.EXPENSE,
                amount,
                description,
                userId,
                companyId
        );

        realtimeEventService.publish("KASA", "CASH_EXPENSE", companyId, tx.getId());

        return tx;
    }


    private CashTransaction createTransaction(TransactionType type,
                                              BigDecimal amount,
                                              String description,
                                              Long userId,
                                              Long companyId) {

        validate(amount);

        amount = amount.setScale(2, RoundingMode.HALF_UP);

        CashTransaction tx =
                CashTransaction.builder()
                        .type(type)
                        .amount(amount)
                        .description(description)
                        .transactionDate(LocalDateTime.now())
                        .userId(userId)
                        .companyId(companyId)
                        .active(true)
                        .build();

        return repository.save(tx);
    }


    public BigDecimal getBalance(Long companyId) {
        return repository.getCurrentBalance(companyId);
    }


    private void validate(BigDecimal amount){
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Amount must be positive");
    }

    public List<CashTransaction> getTransactions(Long userId, Long companyId, ERole role) {

        // ADMIN → tüm işlemler
        if (role == ERole.ADMIN) {
            return repository
                    .findByCompanyIdAndActiveTrueOrderByTransactionDateDesc(
                            companyId
                    );
        }

        // USER → sadece kendi işlemleri
        return repository
                .findByCompanyIdAndUserIdAndActiveTrueOrderByTransactionDateDesc(
                        companyId,
                        userId
                );
    }
}

