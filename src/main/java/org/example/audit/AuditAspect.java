package org.example.audit;

import lombok.RequiredArgsConstructor;
import org.example.entity.AuditLog;
import org.example.repository.AuditLogRepository;
import org.example.security.CustomUserDetails;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint pjp, Audit audit) throws Throwable {

        Object result = pjp.proceed(); // işlem başarılı olursa logla

        CustomUserDetails user = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        BigDecimal amount = null;
        String description = null;

        for (Object arg : pjp.getArgs()) {
            if (arg instanceof BigDecimal bd) amount = bd;
            if (arg instanceof String s && description == null) description = s;
        }

        AuditLog log = AuditLog.builder()
                .username(user.getUsername())          // ✅ username
                .companyId(user.getCompanyId())        // ✅ company restriction için şart
                .action(audit.action())
                .amount(amount)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);

        return result;
    }
}
