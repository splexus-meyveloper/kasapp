package org.example.repository;

import org.example.entity.PriceImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceImportBatchRepository extends JpaRepository<PriceImportBatch, Long> {

    Optional<PriceImportBatch> findByIdAndCompanyId(Long id, Long companyId);
}
