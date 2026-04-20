package org.example.websocket;

import java.time.LocalDateTime;

public record RealtimeEvent(
        String module,
        String action,
        Long companyId,
        Long entityId,
        LocalDateTime time
) {}