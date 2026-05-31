package org.example.repository;

import org.example.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    /** Built-in (companyId = null) + bu şirkete özel kategoriler */
    @Query("SELECT c FROM ExpenseCategory c WHERE c.companyId IS NULL OR c.companyId = :companyId ORDER BY c.label")
    List<ExpenseCategory> findAllForCompany(Long companyId);

    boolean existsByCodeAndCompanyId(String code, Long companyId);

    Optional<ExpenseCategory> findByIdAndCompanyId(Long id, Long companyId);
}
