package org.example.repository;

import org.example.entity.ChangeRequest;
import org.example.skills.enums.ChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    List<ChangeRequest> findByStatusOrderByRequestedAtDesc(ChangeRequestStatus status);
}