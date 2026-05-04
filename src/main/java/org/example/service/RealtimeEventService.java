package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.websocket.RealtimeEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
            return;
        }

        send(event);
    }

    public void publish(String module, Long companyId) {
        publish(module, "UPDATED", companyId, null);
    }

    private void send(RealtimeEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/company-" + event.companyId(),
                event
        );
    }
}
