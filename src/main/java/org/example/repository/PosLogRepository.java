package org.example.repository;

import org.example.entity.PosLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosLogRepository extends JpaRepository<PosLog, Long> {
    Page<PosLog> findByCompanyIdOrderByLogDateDesc(Long companyId, Pageable pageable);
}