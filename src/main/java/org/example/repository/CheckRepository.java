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

    Optional<Check> findByCheckNoAndBankAndDueDateAndCompanyId(
            String checkNo,
            Banka bank,
            LocalDate dueDate,
            Long companyId
    );

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
}
