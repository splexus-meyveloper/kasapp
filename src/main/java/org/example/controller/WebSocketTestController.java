package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.CustomUserDetails;
import org.example.service.RealtimeEventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ws-test")
@RequiredArgsConstructor
public class WebSocketTestController {

    private final RealtimeEventService realtimeEventService;

    /**
     * Admin bu endpoint'i çağırırsa tüm frontend'lere test eventi gönderilir.
     * WebSocket bağlantısını doğrulamak için kullanılır.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ping")
    public String ping(@AuthenticationPrincipal CustomUserDetails user) {
        realtimeEventService.publish("SYSTEM", "WS_PING", user.getCompanyId(), null);
        return "WebSocket ping gönderildi → /topic/company-" + user.getCompanyId();
    }
}