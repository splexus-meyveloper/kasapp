package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.audit.AuditLogSpecifications;
import org.example.dto.response.PageResponse;
import org.example.dto.response.AuditLogResponse;
import org.example.entity.AuditLog;
import org.example.repository.AuditLogRepository;
import org.example.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository repo;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<AuditLogResponse> list(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String q,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end,
            Pageable pageable
    ) {

        if (user == null) {
            throw new RuntimeException("Unauthorized");
        }

        Long companyId = user.getCompanyId();

        Specification<AuditLog> spec = Specification
                .where(AuditLogSpecifications.companyIdEquals(companyId))
                .and(AuditLogSpecifications.usernameContains(username))
                .and(AuditLogSpecifications.actionEquals(action))
                .and(AuditLogSpecifications.amountGte(minAmount))
                .and(AuditLogSpecifications.amountLte(maxAmount))
                .and(AuditLogSpecifications.descriptionContains(q))
                .and(AuditLogSpecifications.createdBetween(start, end));


        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize();


        if (size > 50) size = 50;

        if (page > 1000) page = 1000;

        Pageable safePageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Order.desc("createdAt"))
                );


        Page<AuditLogResponse> pageResult =
                repo.findAll(spec, safePageable)
                        .map(AuditLogResponse::from);

        return new PageResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}
