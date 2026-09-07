package io.todorok.messaging;

import java.util.UUID;

public interface InboxEventGuard {
    boolean claim(UUID eventId, String eventType);
}
