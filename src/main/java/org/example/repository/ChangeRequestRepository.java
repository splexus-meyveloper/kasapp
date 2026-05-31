package org.example.repository;

import org.example.entity.ChangeRequest;
import org.example.skills.enums.ChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    List<ChangeRequest> findByCompanyIdAndStatusOrderByRequestedAtDesc(
            Long companyId, ChangeRequestStatus status);

    List<ChangeRequest> findByStatusOrderByRequestedAtDesc(ChangeRequestStatus status);

    // Limitsiz olan sorgu kaldırıldı, yerine Pageable versiyonu eklendi
    Page<ChangeRequest> findByCompanyIdAndRequestedByOrderByRequestedAtDesc(
            Long companyId, Long requestedBy, Pageable pageable);

    boolean existsByCompanyIdAndEntityTypeAndEntityIdAndStatus(
            Long companyId, String entityType, Long entityId, ChangeRequestStatus status);

    List<ChangeRequest> findAllByOrderByRequestedAtDesc();

    List<ChangeRequest> findByCompanyIdOrderByRequestedAtDesc(Long companyId);
}
