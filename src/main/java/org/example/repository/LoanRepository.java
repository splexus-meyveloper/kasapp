package org.example.repository;

import org.example.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByCompanyId(Long companyId);

    List<Loan> findByCompanyIdAndActiveTrue(Long companyId);

    // Loan toplamını Java'da değil DB'de hesapla
    @Query("SELECT COALESCE(SUM(l.remainingDebt), 0) FROM Loan l WHERE l.companyId = :companyId AND l.active = true")
    BigDecimal sumRemainingDebtByCompanyId(Long companyId);
}