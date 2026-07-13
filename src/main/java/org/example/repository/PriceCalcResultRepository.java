package org.example.repository;

import org.example.entity.PriceCalcResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceCalcResultRepository extends JpaRepository<PriceCalcResult, Long> {

    List<PriceCalcResult> findByCalcRunId(Long calcRunId);

    List<PriceCalcResult> findByCalcRunIdAndMatched(Long calcRunId, boolean matched);
}
