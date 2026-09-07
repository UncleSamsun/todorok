package io.todorok.messaging;

import io.todorok.contracts.*;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.*;

/** Rejects malformed envelopes before an inbox claim can become durable. */
public final class EventJson {

    private EventJson() {}

    public static EventEnvelope<JsonNode> read(
        ObjectMapper mapper,
        String json
    ) {
        JsonNode n = mapper.readTree(json);
        if (
            !n.path("version").isIntegralNumber() ||
            (n.path("version").asInt() != 1 && !(n.path("version").asInt() == 2
                && java.util.Set.of("TASK_SCHEDULED", "TASK_CHANGED", "TASK_ROLLED_OVER", "SERIES_CHANGED")
                    .contains(n.path("type").asText()))) ||
            !n.path("aggregateVersion").isIntegralNumber() ||
            !n.path("aggregateVersion").canConvertToLong() ||
            !n.path("payload").isObject()
        ) throw new IllegalArgumentException("Invalid event envelope");
        return new EventEnvelope<>(
            uuid(n, "eventId"),
            EventType.valueOf(text(n, "type")),
            n.get("version").asInt(),
            n.get("aggregateVersion").longValue(),
            Instant.parse(text(n, "occurredAt")),
            uuid(n, "userId"),
            n.get("payload")
        );
    }

    public static String text(JsonNode n, String key) {
        if (
            !n.path(key).isTextual() || n.get(key).textValue().isBlank()
        ) throw new IllegalArgumentException("Missing text: " + key);
        return n.get(key).textValue();
    }

    public static UUID uuid(JsonNode n, String key) {
        return UUID.fromString(text(n, key));
    }
}
