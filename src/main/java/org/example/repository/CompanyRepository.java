package org.example.repository;

import org.example.entity.Company;
import org.example.skills.enums.BranchType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByCode(String code);

    boolean existsByCode(String code);

    List<Company> findByBranchType(BranchType branchType);

    Optional<Company> findFirstByBranchType(BranchType branchType);
}
