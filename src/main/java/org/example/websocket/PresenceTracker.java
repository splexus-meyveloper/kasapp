package org.example.websocket;

import org.example.security.CustomUserDetails;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket oturumları üzerinden "şu an aktif" kullanıcı takibi.
 * Uygulamayı açan her kullanıcı STOMP bağlantısı kurduğu için
 * açık bağlantı listesi = aktif oturum listesi.
 */
@Component
public class PresenceTracker {

    public record ActiveUser(Long userId, String username, String role,
                             Long companyId, LocalDateTime connectedAt) {}

    private final Map<String, ActiveUser> sessions = new ConcurrentHashMap<>();

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        CustomUserDetails ud = extractUser(event.getUser());
        if (sessionId == null || ud == null) return;
        sessions.put(sessionId, new ActiveUser(
                ud.getId(), ud.getUsername(), ud.getRole(), ud.getCompanyId(), LocalDateTime.now()));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) sessions.remove(sessionId);
    }

    private CustomUserDetails extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof CustomUserDetails ud) {
            return ud;
        }
        return null;
    }

    /** Aktif kullanıcılar — aynı kullanıcı birden fazla sekme açtıysa en eski bağlantı esas alınır. */
    public List<ActiveUser> getActiveUsers() {
        Map<Long, ActiveUser> byUser = new LinkedHashMap<>();
        sessions.values().stream()
                .sorted(Comparator.comparing(ActiveUser::connectedAt))
                .forEach(u -> byUser.putIfAbsent(u.userId(), u));
        return new ArrayList<>(byUser.values());
    }
}
