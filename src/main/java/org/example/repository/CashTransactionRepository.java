package org.example.repository;

import org.example.entity.CashTransaction;
import org.example.skills.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

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
      and (t.transferTransaction is null or t.transferTransaction = false)
      and (t.description is null or lower(t.description) not like '%transfer #%')
      and t.type = :incomeType
      and t.transactionDate >= :start
      and t.transactionDate < :end
    """)
    BigDecimal sumTodayIncome(Long companyId, TransactionType incomeType,
                              LocalDateTime start, LocalDateTime end);

    @Query("""
    select coalesce(sum(t.amount), 0)
    from CashTransaction t
    where t.companyId = :companyId
      and t.active = true
      and (t.transferTransaction is null or t.transferTransaction = false)
      and (t.description is null or lower(t.description) not like '%transfer #%')
      and t.type = :expenseType
      and t.transactionDate >= :start
      and t.transactionDate < :end
    """)
    BigDecimal sumTodayExpense(Long companyId, TransactionType expenseType,
                               LocalDateTime start, LocalDateTime end);

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
      and (t.transferTransaction is null or t.transferTransaction = false)
      and (t.description is null or lower(t.description) not like '%transfer #%')
      and t.transactionDate >= :start
      and t.transactionDate < :end
    """)
    BigDecimal monthlyNet(Long companyId, TransactionType incomeType,
                          LocalDateTime start, LocalDateTime end);

    @Query("""
    select coalesce(sum(
        case
          when t.type = 'INCOME' then t.amount
          else -t.amount
        end
    ), 0)
    from CashTransaction t
    where t.companyId = :companyId
      and t.active = true
    """)
    BigDecimal balance(Long companyId);

    @Query("""
    select coalesce(sum(t.amount), 0)
    from CashTransaction t
    where t.companyId = :companyId
      and t.userId = :userId
      and t.active = true
      and (t.transferTransaction is null or t.transferTransaction = false)
      and (t.description is null or lower(t.description) not like '%transfer #%')
      and t.type = :incomeType
      and t.transactionDate >= :start
      and t.transactionDate < :end
    """)
    BigDecimal sumTodayIncomeByUser(Long companyId, Long userId, TransactionType incomeType,
                                    LocalDateTime start, LocalDateTime end);

    @Query("""
    select coalesce(sum(t.amount), 0)
    from CashTransaction t
    where t.companyId = :companyId
      and t.userId = :userId
      and t.active = true
      and (t.transferTransaction is null or t.transferTransaction = false)
      and (t.description is null or lower(t.description) not like '%transfer #%')
      and t.type = :expenseType
      and t.transactionDate >= :start
      and t.transactionDate < :end
    """)
    BigDecimal sumTodayExpenseByUser(Long companyId, Long userId, TransactionType expenseType,
                                     LocalDateTime start, LocalDateTime end);

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
      and (t.transferTransaction is null or t.transferTransaction = false)
      and (t.description is null or lower(t.description) not like '%transfer #%')
      and t.transactionDate >= :start
      and t.transactionDate < :end
    """)
    BigDecimal monthlyNetByUser(Long companyId, Long userId, TransactionType incomeType,
                                LocalDateTime start, LocalDateTime end);

    // ── Grafik için: 7 günü tek sorguda çek ──────────────────────────────
    @Query("""
    select
        cast(t.transactionDate as date) as day,
        t.type,
        coalesce(sum(t.amount), 0) as total
    from CashTransaction t
    where t.companyId = :companyId
      and t.active = true
      and (t.transferTransaction is null or t.transferTransaction = false)
      and (t.description is null or lower(t.description) not like '%transfer #%')
      and t.transactionDate >= :start
      and t.transactionDate < :end
    group by cast(t.transactionDate as date), t.type
    order by cast(t.transactionDate as date) asc
    """)
    List<Object[]> sumByDayAndType(Long companyId, LocalDateTime start, LocalDateTime end);

    @Query("""
    select
        cast(t.transactionDate as date) as day,
        t.type,
        coalesce(sum(t.amount), 0) as total
    from CashTransaction t
    where t.companyId = :companyId
      and t.userId = :userId
      and t.active = true
      and (t.transferTransaction is null or t.transferTransaction = false)
      and (t.description is null or lower(t.description) not like '%transfer #%')
      and t.transactionDate >= :start
      and t.transactionDate < :end
    group by cast(t.transactionDate as date), t.type
    order by cast(t.transactionDate as date) asc
    """)
    List<Object[]> sumByDayAndTypeForUser(Long companyId, Long userId,
                                          LocalDateTime start, LocalDateTime end);

    // ── Pagination ile liste ──────────────────────────────────────────────
    Page<CashTransaction> findByCompanyIdAndActiveTrueOrderByTransactionDateDesc(
            Long companyId, Pageable pageable);

    Page<CashTransaction> findByCompanyIdAndUserIdAndActiveTrueOrderByTransactionDateDesc(
            Long companyId, Long userId, Pageable pageable);

    // Global arama — açıklamada geçen metin (tüm şirket, admin)
    @Query("""
        SELECT t FROM CashTransaction t
        WHERE t.companyId = :companyId
          AND t.active = true
          AND LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY t.transactionDate DESC
    """)
    List<CashTransaction> searchForGlobal(Long companyId, String q, Pageable pageable);

    // Global arama — kullanıcının kendi işlemleri
    @Query("""
        SELECT t FROM CashTransaction t
        WHERE t.companyId = :companyId
          AND t.userId = :userId
          AND t.active = true
          AND LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY t.transactionDate DESC
    """)
    List<CashTransaction> searchForGlobalByUser(Long companyId, Long userId, String q, Pageable pageable);
}
