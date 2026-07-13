package org.example.repository;

import org.example.entity.StockSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockSnapshotRepository extends JpaRepository<StockSnapshot, Long> {

    List<StockSnapshot> findByBatchId(Long batchId);
}
