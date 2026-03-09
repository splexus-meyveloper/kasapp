package org.example.repository;

import org.example.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.repository.LoanRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByCompanyId(Long companyId);

    List<Loan> findByCompanyIdAndActiveTrue(Long companyId);
}
