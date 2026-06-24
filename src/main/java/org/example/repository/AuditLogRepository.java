package org.example.repository;

import org.example.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    Page<AuditLog> findByUsernameAndCompanyIdOrderByCreatedAtDesc(
            String username, Long companyId, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndUserIdOrderByCreatedAtDesc(
            Long companyId, Long userId, Pageable pageable);

    void deleteByEntityIdAndEntityType(Long entityId, String entityType);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.companyId = :companyId
          AND a.userId = :userId
          AND (CAST(:action AS string) IS NULL OR a.action = :action)
          AND (CAST(:start AS java.time.LocalDateTime) IS NULL OR a.createdAt >= :start)
          AND (CAST(:end AS java.time.LocalDateTime) IS NULL OR a.createdAt < :end)
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> findFiltered(
            @Param("companyId") Long companyId,
            @Param("userId")    Long userId,
            @Param("action")    String action,
            @Param("start")     LocalDateTime start,
            @Param("end")       LocalDateTime end,
            Pageable pageable
    );
}