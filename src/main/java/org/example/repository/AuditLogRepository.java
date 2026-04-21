package org.example.repository;

import org.example.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    List<AuditLog> findByCompanyIdAndUserIdOrderByCreatedAtDesc(Long companyId, Long userId);

}
