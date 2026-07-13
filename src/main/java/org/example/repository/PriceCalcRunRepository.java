package org.example.repository;

import org.example.entity.PriceCalcRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceCalcRunRepository extends JpaRepository<PriceCalcRun, Long> {

    Optional<PriceCalcRun> findByIdAndCompanyId(Long id, Long companyId);
}
