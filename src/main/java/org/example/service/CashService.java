package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.entity.CashTransaction;
import org.example.repository.CashTransactionRepository;
import org.example.skills.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CashService {

    private final CashTransactionRepository repository;


    @Audit(action = "CASH_INCOME")
    @Transactional
    public CashTransaction addIncome(BigDecimal amount,
                                     String description,
                                     Long userId,
                                     Long companyId) {

        return createTransaction(
                TransactionType.INCOME,
                amount,
                description,
                userId,
                companyId
        );
    }


    @Audit(action = "CASH_EXPENSE")
    @Transactional
    public CashTransaction addExpense(BigDecimal amount,
                                      String description,
                                      Long userId,
                                      Long companyId) {

        return createTransaction(
                TransactionType.EXPENSE,
                amount,
                description,
                userId,
                companyId
        );
    }


    @Transactional
    public CashTransaction addExpenseFromExpenseModule(BigDecimal amount,
                                                       String description,
                                                       Long userId,
                                                       Long companyId) {

        return createTransaction(
                TransactionType.EXPENSE,
                amount,
                description,
                userId,
                companyId
        );
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
}

