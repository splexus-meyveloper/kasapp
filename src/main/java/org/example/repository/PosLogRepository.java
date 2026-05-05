package org.example.repository;

import org.example.entity.PosLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PosLogRepository extends JpaRepository<PosLog, Long> {
    Page<PosLog> findByCompanyIdOrderByLogDateDesc(Long companyId, Pageable pageable);

    Optional<PosLog> findByIdAndCompanyId(Long id, Long companyId);

    List<PosLog> findByCompanyIdAndUserIdOrderByLogDateDesc(
            Long companyId, Long userId, Pageable pageable);

    List<PosLog> findByCompanyIdAndUserIdAndLogDateGreaterThanEqualOrderByLogDateDesc(
            Long companyId, Long userId, LocalDateTime start, Pageable pageable);

    List<PosLog> findByCompanyIdAndUserIdAndLogDateLessThanOrderByLogDateDesc(
            Long companyId, Long userId, LocalDateTime end, Pageable pageable);

    List<PosLog> findByCompanyIdAndUserIdAndLogDateGreaterThanEqualAndLogDateLessThanOrderByLogDateDesc(
            Long companyId, Long userId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
