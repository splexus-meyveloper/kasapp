package org.example.repository;

import org.example.entity.Expense;
import org.example.skills.enums.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByCompanyIdOrderByExpenseDateDesc(Long companyId);

    // Tarih aralığında kategori bazlı masraf toplamı
    @Query("""
        SELECT e.type, COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.companyId = :companyId
          AND e.expenseDate >= :start
          AND e.expenseDate <= :end
        GROUP BY e.type
    """)
    List<Object[]> sumByTypeAndDateRange(Long companyId, LocalDate start, LocalDate end);

    // Tarih aralığında toplam masraf
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.companyId = :companyId
          AND e.expenseDate >= :start
          AND e.expenseDate <= :end
    """)
    BigDecimal sumTotalExpense(Long companyId, LocalDate start, LocalDate end);
}