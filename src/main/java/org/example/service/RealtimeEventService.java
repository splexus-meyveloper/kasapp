package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.websocket.RealtimeEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(String module, String action, Long companyId, Long entityId) {
        RealtimeEvent event = new RealtimeEvent(
                module,
                action,
                companyId,
                entityId,
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend(
                "/topic/company-" + companyId,
                event
        );
    }

    public void publish(String module, Long companyId) {
        publish(module, "UPDATED", companyId, null);
    }
}