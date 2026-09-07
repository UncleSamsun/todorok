package io.todorok.messaging.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DebeziumOutboxRoundTripTest {

    private static MessagingInfrastructureFixture fixture;

    @BeforeAll
    static void startInfrastructure() {
        fixture = MessagingInfrastructureFixture.start();
    }

    @AfterAll
    static void stopInfrastructure() {
        if (fixture != null) fixture.close();
    }

    @Test
    void routesPlannerAndActivityEnvelopes() {
        var taskEventId = UUID.fromString("00000000-0000-0000-0000-000000000401");
        var activityEventId = UUID.fromString("00000000-0000-0000-0000-000000000402");

        fixture.insertOutbox(
                "planner", taskEventId, "task", "task-1",
                "TASK_CHANGED", envelope(taskEventId, "TASK_CHANGED"));
        fixture.insertOutbox(
                "activity", activityEventId, "activity", "activity-1",
                "ACTIVITY_COMPLETED", envelope(activityEventId, "ACTIVITY_COMPLETED"));

        var task = fixture.consume("todorok.task.v1", taskEventId, Duration.ofSeconds(20));
        var activity = fixture.consume(
                "todorok.activity.v1", activityEventId, Duration.ofSeconds(20));
        assertThat(task.key()).isEqualTo("task-1");
        assertThat(task.value().path("eventId").asText()).isEqualTo(taskEventId.toString());
        assertThat(activity.key()).isEqualTo("activity-1");
        assertThat(activity.value().path("eventId").asText())
                .isEqualTo(activityEventId.toString());
    }

    @Test
    void preservesOrderForSameAggregateAndIgnoresOtherTables() {
        var first = UUID.fromString("00000000-0000-0000-0000-000000000411");
        var second = UUID.fromString("00000000-0000-0000-0000-000000000412");
        fixture.insertOutbox(
                "planner", first, "task", "task-order",
                "TASK_CHANGED", envelope(first, "TASK_CHANGED"));
        fixture.insertOutbox(
                "planner", second, "task", "task-order",
                "TASK_CHANGED", envelope(second, "TASK_CHANGED"));
        fixture.updateServiceMetadata("planner", 2);

        var firstRecord = fixture.consume(
                "todorok.task.v1", first, Duration.ofSeconds(20));
        var secondRecord = fixture.consume(
                "todorok.task.v1", second, Duration.ofSeconds(20));
        assertThat(firstRecord.offset()).isLessThan(secondRecord.offset());
        assertThat(fixture.hasCdcRecordForServiceMetadata(Duration.ofSeconds(2))).isFalse();
    }

    private String envelope(UUID eventId, String type) {
        return """
                {
                  "eventId": "%s",
                  "type": "%s",
                  "version": 1,
                  "occurredAt": "2026-09-02T00:00:00Z",
                  "userId": "00000000-0000-0000-0000-000000000499",
                  "payload": {"probe": true}
                }
                """.formatted(eventId, type);
    }
}
