package org.example.repository;

import org.example.entity.Check;
import org.example.skills.enums.Banka;
import org.example.skills.enums.CheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface CheckRepository extends JpaRepository<Check,Long> {
    boolean existsByCheckNoAndCompanyId(
            String checkNo,
            Long companyId
    );

    List<Check> findByCompanyId(Long companyId);

    Optional<Check> findByCheckNoAndBankAndDueDateAndCompanyId(
            String checkNo,
            Banka bank,
            LocalDate dueDate,
            Long companyId
    );

    Optional<Check> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
   SELECT COALESCE(SUM(c.amount),0)
   FROM Check c
   WHERE c.status='PORTFOYDE'
   AND c.companyId=:companyId
""")
    BigDecimal getPortfolioTotal(Long companyId);

    List<Check> findByStatusAndCompanyId(
            CheckStatus status,
            Long companyId
    );

    // Vadesi yaklaşan çekler (x gün içinde)
    @Query("""
    SELECT c FROM Check c
    WHERE c.companyId = :companyId
      AND c.dueDate >= :today
      AND c.dueDate <= :limitDate
    ORDER BY c.dueDate ASC
""")
    List<Check> findUpcomingDue(Long companyId, LocalDate today, LocalDate limitDate);

    // Tarih aralığında çek toplamı
    @Query("""
    SELECT COALESCE(SUM(c.amount), 0)
    FROM Check c
    WHERE c.companyId = :companyId
      AND c.dueDate >= :start
      AND c.dueDate <= :end
""")
    BigDecimal sumByDateRange(Long companyId, LocalDate start, LocalDate end);
}