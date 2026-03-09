package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.AuditLog;
import org.example.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    public Page<AuditLog> getUserLogs(String username, int page, int size){

        PageRequest pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by("createdAt").descending()
                );

        return repository.findByUsernameOrderByCreatedAtDesc(
                username,
                pageable
        );
    }
}
