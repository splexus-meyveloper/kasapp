package org.example.repository;

import org.example.entity.PriceImportRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PriceImportRowRepository extends JpaRepository<PriceImportRow, Long> {

    List<PriceImportRow> findByBatchId(Long batchId);

    Optional<PriceImportRow> findByIdAndBatchId(Long id, Long batchId);

    /** Manuel eşleştirme arama kutusu — üretici kodu veya açıklamaya göre, bu batch içinde. */
    @Query("""
        SELECT r FROM PriceImportRow r
        WHERE r.batchId = :batchId
          AND (LOWER(r.manufacturerCode) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(r.description) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY r.manufacturerCode ASC
    """)
    List<PriceImportRow> search(Long batchId, String q, Pageable pageable);
}
