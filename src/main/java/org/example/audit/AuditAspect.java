package org.example.audit;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.CheckEntryRequest;
import org.example.dto.request.CheckExitRequest;
import org.example.entity.AuditLog;
import org.example.entity.Check;
import org.example.repository.AuditLogRepository;
import org.example.repository.CheckRepository;
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
    private final CheckRepository checkRepository;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint pjp, Audit audit) throws Throwable {

        Object result = pjp.proceed();

        CustomUserDetails user = (CustomUserDetails)
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        BigDecimal amount = null;
        String description = null;

        for (Object arg : pjp.getArgs()) {

            // ✅ CashService vb için eski destek
            if (arg instanceof BigDecimal bd)
                amount = bd;

            if (arg instanceof String s && description == null)
                description = s;

            // ✅ DTO desteği (BURASI ÇÖZÜM)
            if (arg instanceof org.example.dto.request.AddExpenseRequest req) {
                amount = req.amount();
                description = req.expenseType().name()
                        + " - "
                        + req.description();
            }

            if (arg instanceof CheckEntryRequest req) {

                amount = req.amount();

                String userDesc =
                        req.description() == null || req.description().isBlank()
                                ? ""
                                : " (" + req.description() + ")";

                description =
                        req.bank() + " "
                                + req.checkNo()
                                + " nolu "
                                + req.amount() + " TL tutarlı çek alındı"
                                + userDesc;
            }


            if (arg instanceof CheckExitRequest req) {

                Check c = checkRepository
                        .findByCheckNoAndBankAndDueDateAndCompanyId(
                                req.checkNo(),
                                req.bank(),
                                req.dueDate(),
                                user.getCompanyId()
                        ).orElse(null);

                BigDecimal amt = c != null ? c.getAmount() : BigDecimal.ZERO;

                amount = amt;

                String userDesc =
                        req.description() == null || req.description().isBlank()
                                ? ""
                                : " (" + req.description() + ")";

                description =
                        req.bank() + " "
                                + req.checkNo()
                                + " nolu "
                                + amt + " TL tutarlı çek portföyden çıkarıldı"
                                + userDesc;
            }
        }

        AuditLog log = AuditLog.builder()
                .username(user.getUsername())
                .companyId(user.getCompanyId())
                .action(audit.action())
                .amount(amount)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);

        return result;
    }
}
