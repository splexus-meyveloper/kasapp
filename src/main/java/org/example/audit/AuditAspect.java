package org.example.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CheckEntryRequest;
import org.example.dto.request.CheckExitRequest;
import org.example.dto.request.NoteEntryRequest;
import org.example.dto.request.NoteExitRequest;
import org.example.entity.AuditLog;
import org.example.entity.Check;
import org.example.entity.Note;
import org.example.repository.AuditLogRepository;
import org.example.repository.CheckRepository;
import org.example.repository.NoteRepository;
import org.example.security.CustomUserDetails;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final CheckRepository checkRepository;
    private final NoteRepository noteRepository;
    private final ObjectMapper objectMapper;


    private Map<String, Object> extractPayload(Object arg) {
        Map<String, Object> payload = new HashMap<>();

        if (arg == null) return payload;

        Package pkg = arg.getClass().getPackage();
        if (pkg == null || !pkg.getName().startsWith("org.example.dto"))
            return payload;

        if (arg.getClass().isRecord()) {
            for (RecordComponent rc : arg.getClass().getRecordComponents()) {
                try {
                    Object val = rc.getAccessor().invoke(arg);

                    // description payload’a girmez
                    if(rc.getName().equals("description"))
                        continue;

                    // 🔥 BURASI ÖNEMLİ
                    if (val instanceof LocalDate ld) {
                        payload.put(rc.getName(), ld.toString());
                    }
                    else if (val instanceof LocalDateTime ldt) {
                        payload.put(rc.getName(), ldt.toString());
                    }
                    else {
                        payload.put(rc.getName(), val);
                    }

                } catch (Exception ignored) {}
            }
        }

        return payload;
    }

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint pjp, Audit audit) throws Throwable {

        Map<String, Object> payload = new HashMap<>();

        Object result = pjp.proceed(); // 🔥 önce metodu çalıştır

        CustomUserDetails user = (CustomUserDetails)
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        BigDecimal amount = null;
        String description = null;

        // ==============================
        // 1️⃣ DTO'lardan yakala (mevcut sistemin)
        // ==============================
        for(Object arg : pjp.getArgs()){

            payload.putAll(extractPayload(arg));
            if(arg == null) continue;

            Package pkg = arg.getClass().getPackage();

            if(pkg == null ||
                    !pkg.getName().startsWith("org.example.dto"))
                continue;

            for(Field f : arg.getClass().getDeclaredFields()){

                f.setAccessible(true);

                if(f.isAnnotationPresent(AuditAmount.class)){
                    Object val = f.get(arg);
                    if(val instanceof BigDecimal bd)
                        amount = bd;
                }

                if(f.isAnnotationPresent(AuditDesc.class)){
                    Object val = f.get(arg);
                    if(val instanceof String s)
                        description = s;
                }
            }
        }

        // ==============================
        // 2️⃣ DTO’da yoksa → result’tan al
        // ==============================
        if(amount == null && result != null){
            try{
                Method m = result.getClass().getMethod("getAmount");
                Object val = m.invoke(result);

                if(val instanceof BigDecimal bd){
                    amount = bd;
                }
            }catch (Exception ignored){}
        }

        if(description == null && result != null){
            try{
                Method m = result.getClass().getMethod("getDescription");
                Object val = m.invoke(result);

                if(val instanceof String s){
                    description = s;
                }
            }catch (Exception ignored){}
        }

        // ==============================
        // 3️⃣ Log oluştur
        // ==============================
        AuditDetails details = AuditDetails.builder()
                .action(audit.action())
                .amount(amount)
                .description(description)
                .userId(user.getId())
                .username(user.getUsername())
                .time(LocalDateTime.now())
                .payload(payload)
                .build();

        AuditLog log = AuditLog.builder()
                .username(user.getUsername())
                .companyId(user.getCompanyId())
                .action(audit.action())
                .amount(amount)
                .detailsJson(details)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);

        return result;
    }

}
