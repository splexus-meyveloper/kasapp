package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.audit.AuditDetails;
import org.example.entity.AuditLog;
import org.example.entity.User;
import org.example.repository.AuditLogRepository;
import org.example.repository.UserRepository;
import org.example.skills.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final UserRepository userRepository;

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

    // 🔥 ANA LOG METHOD
    public void log(
            AuditAction action,
            String entityType,
            Long entityId,
            Long userId,
            Long companyId,
            Map<String, Object> payload
    ) {

        User user = userRepository.findById(userId).orElse(null);

        String username = user != null ? user.getUsername() : "UNKNOWN";

        AuditDetails details = AuditDetails.builder()
                .action(action.name())
                .userId(userId)
                .username(username)
                .time(LocalDateTime.now())
                .payload(payload)
                .build();

        AuditLog log = AuditLog.builder()
                .action(action.name())
                .entityType(entityType)
                .entityId(entityId)
                .userId(userId)
                .username(username)
                .companyId(companyId)
                .createdAt(LocalDateTime.now())
                .detailsJson(details)
                .build();

        repository.save(log);
    }
}