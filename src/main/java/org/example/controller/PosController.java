package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.PosLogRequest;
import org.example.dto.response.PosLogResponse;
import org.example.dto.response.PosTerminalGroupResponse;
import org.example.security.CustomUserDetails;
import org.example.service.PosService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosService service;

    @PreAuthorize("hasAuthority('KASA') or hasRole('ADMIN')")
    @GetMapping("/terminals")
    public List<PosTerminalGroupResponse> getTerminals() {
        return service.getTerminals();
    }

    @PreAuthorize("hasAuthority('KASA') or hasRole('ADMIN')")
    @PostMapping("/log")
    public ResponseEntity<PosLogResponse> logPos(
            @Valid @RequestBody PosLogRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        PosLogRequest effectiveReq = hasGecmisTarih(user) ? req
                : new PosLogRequest(req.posType(), req.terminal(), req.amount(), req.description(), null);
        return ResponseEntity.ok(service.logPos(effectiveReq, user.getId(), user.getCompanyId()));
    }

    /** Log listesi — sadece admin */
    @PreAuthorize("hasAuthority('KASA') or hasRole('ADMIN')")
    @GetMapping("/logs")
    public List<PosLogResponse> getLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "false") boolean includeAll,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal CustomUserDetails user) {
        boolean canIncludeAll = includeAll && "ADMIN".equalsIgnoreCase(user.getRole());
        LocalDate resolvedStart = firstDate(startDate, dateFrom, start, from);
        LocalDate resolvedEnd = firstDate(endDate, dateTo, end, to);

        return service.getLogs(
                user.getCompanyId(),
                user.getId(),
                canIncludeAll,
                resolvedStart != null ? resolvedStart.atStartOfDay() : null,
                resolvedEnd != null ? resolvedEnd.plusDays(1).atStartOfDay() : null,
                page,
                size
        );
    }

    private LocalDate firstDate(String... values) {
        for (String value : values) {
            LocalDate parsed = parseDate(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private boolean hasGecmisTarih(CustomUserDetails user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> "GECMIS_TARIH".equals(a.getAuthority()));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() >= 10 && normalized.charAt(4) == '-') {
            normalized = normalized.substring(0, 10);
        }

        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (Exception ignored) {
            return null;
        }
    }
}
