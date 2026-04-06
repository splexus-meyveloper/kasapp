package org.example.repository;

import org.example.entity.CashTransaction;
import org.example.skills.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CashTransactionRepository extends JpaRepository<CashTransaction,Long> {
    @Query("""
    SELECT COALESCE(SUM(
        CASE 
            WHEN c.type = 'INCOME' THEN c.amount
            ELSE -c.amount
        END
    ), 0)
    FROM CashTransaction c
    WHERE c.active = true
      AND c.companyId = :companyId
    """)
    BigDecimal getCurrentBalance(Long companyId);

    @Query("""
select coalesce(sum(t.amount), 0)
from CashTransaction t
where t.companyId = :companyId
  and t.active = true
  and t.type = :incomeType
  and t.transactionDate >= :start
  and t.transactionDate < :end
""")
    BigDecimal sumTodayIncome(Long companyId,
                              TransactionType incomeType,
                              LocalDateTime start,
                              LocalDateTime end);

    @Query("""
select coalesce(sum(t.amount), 0)
from CashTransaction t
where t.companyId = :companyId
  and t.active = true
  and t.type = :expenseType
  and t.transactionDate >= :start
  and t.transactionDate < :end
""")
    BigDecimal sumTodayExpense(Long companyId,
                               TransactionType expenseType,
                               LocalDateTime start,
                               LocalDateTime end);

    @Query("""
select coalesce(sum(
    case
      when t.type = :incomeType then t.amount
      else -t.amount
    end
), 0)
from CashTransaction t
where t.companyId = :companyId
  and t.active = true
  and t.transactionDate >= :start
  and t.transactionDate < :end
""")
    BigDecimal monthlyNet(Long companyId,
                          TransactionType incomeType,
                          LocalDateTime start,
                          LocalDateTime end);

    @Query("""
select coalesce(sum(
    case
      when t.type = :incomeType then t.amount
      else -t.amount
    end
), 0)
from CashTransaction t
where t.companyId = :companyId
  and t.active = true
""")
    BigDecimal balance(Long companyId, TransactionType incomeType);

    @Query("""
select coalesce(sum(t.amount), 0)
from CashTransaction t
where t.companyId = :companyId
  and t.userId = :userId
  and t.active = true
  and t.type = :incomeType
  and t.transactionDate >= :start
  and t.transactionDate < :end
""")
    BigDecimal sumTodayIncomeByUser(Long companyId,
                                    Long userId,
                                    TransactionType incomeType,
                                    LocalDateTime start,
                                    LocalDateTime end);

    @Query("""
select coalesce(sum(t.amount), 0)
from CashTransaction t
where t.companyId = :companyId
  and t.userId = :userId
  and t.active = true
  and t.type = :expenseType
  and t.transactionDate >= :start
  and t.transactionDate < :end
""")
    BigDecimal sumTodayExpenseByUser(Long companyId,
                                     Long userId,
                                     TransactionType expenseType,
                                     LocalDateTime start,
                                     LocalDateTime end);

    List<CashTransaction> findByCompanyIdAndUserIdAndActiveTrueOrderByTransactionDateDesc(
            Long companyId,
            Long userId
    );

    List<CashTransaction> findByCompanyIdAndActiveTrueOrderByTransactionDateDesc(
            Long companyId
    );

    @Query("""
select coalesce(sum(
    case
      when t.type = :incomeType then t.amount
      else -t.amount
    end
), 0)
from CashTransaction t
where t.companyId = :companyId
  and t.userId = :userId
  and t.active = true
  and t.transactionDate >= :start
  and t.transactionDate < :end
""")
    BigDecimal monthlyNetByUser(Long companyId,
                                Long userId,
                                TransactionType incomeType,
                                LocalDateTime start,
                                LocalDateTime end);

}
