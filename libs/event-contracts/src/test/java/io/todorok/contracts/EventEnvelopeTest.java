package io.todorok.contracts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void preservesIdentityAndVersionDuringJsonRoundTrip() throws Exception {
        var envelope = new EventEnvelope<>(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                EventType.TASK_SCHEDULED,
                1,
                Instant.parse("2026-08-31T00:00:00Z"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                Map.of("taskId", "00000000-0000-0000-0000-000000000003"));

        var json = mapper.writeValueAsString(envelope);
        var restored = mapper.readTree(json);

        assertThat(restored.path("eventId").asText()).isEqualTo(envelope.eventId().toString());
        assertThat(restored.path("type").asText()).isEqualTo("TASK_SCHEDULED");
        assertThat(restored.path("version").asInt()).isEqualTo(1);
        assertThat(restored.path("occurredAt").asText()).isEqualTo("2026-08-31T00:00:00Z");
        assertThat(restored.path("userId").asText()).isEqualTo(envelope.userId().toString());
    }

    @Test
    void rejectsVersionLowerThanOne() {
        assertThatThrownBy(() -> envelopeWith(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must be at least 1");
    }

    @Test
    void rejectsMissingEventId() {
        assertThatThrownBy(() -> envelopeWith(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventId must not be null");
    }

    private EventEnvelope<Map<String, String>> envelopeWith(UUID eventId, int version) {
        return new EventEnvelope<>(
                eventId,
                EventType.TASK_SCHEDULED,
                version,
                Instant.parse("2026-08-31T00:00:00Z"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                Map.of("taskId", "00000000-0000-0000-0000-000000000003"));
    }
}
