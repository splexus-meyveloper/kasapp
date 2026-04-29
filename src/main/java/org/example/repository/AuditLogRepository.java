package org.example.repository;

import org.example.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndUserIdOrderByCreatedAtDesc(
            Long companyId, Long userId, Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.companyId = :companyId
          AND a.userId = :userId
          AND (:action IS NULL OR a.action = :action)
          AND (:start  IS NULL OR a.createdAt >= :start)
          AND (:end    IS NULL OR a.createdAt < :end)
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> findFiltered(
            Long companyId,
            Long userId,
            String action,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );
}