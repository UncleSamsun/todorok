package io.todorok.notification.push;

import java.time.Instant;
import java.util.UUID;

/** Provider transport boundary: returns HTTP status, or zero for a transient transport failure. */
public interface PushSender {
    int send(PushSecrets.Secret subscription, UUID deliveryId, Instant deadline);
}
