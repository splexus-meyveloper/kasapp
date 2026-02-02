package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.entity.CashTransaction;
import org.example.repository.CashTransactionRepository;
import org.example.skills.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CashService {
    private final CashTransactionRepository repository;

    @Audit(action = "CASH_INCOME")
    @Transactional
    public void addIncome(BigDecimal amount,
                          String description,
                          Long userId,
                          Long companyId) {

        validate(amount);

        CashTransaction tx =
                CashTransaction.builder()
                        .type(TransactionType.INCOME)
                        .amount(amount)
                        .description(description)
                        .transactionDate(LocalDateTime.now())
                        .userId(userId)
                        .companyId(companyId)
                        .active(true)
                        .build();

        repository.save(tx);
    }

    @Audit(action = "CASH_EXPENSE")
    @Transactional
    public void addExpense(BigDecimal amount,
                           String description,
                           Long userId,
                           Long companyId) {

        validate(amount);

        CashTransaction tx =
                CashTransaction.builder()
                        .type(TransactionType.EXPENSE)
                        .amount(amount)
                        .description(description)
                        .transactionDate(LocalDateTime.now())
                        .userId(userId)
                        .companyId(companyId)
                        .active(true)
                        .build();

        repository.save(tx);
    }

    public BigDecimal getBalance() {
        return repository.getCurrentBalance();
    }

    private void validate(BigDecimal amount){
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Amount must be positive");
    }
}
