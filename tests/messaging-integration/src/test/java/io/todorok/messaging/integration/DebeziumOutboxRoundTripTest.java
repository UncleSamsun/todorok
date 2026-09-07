package io.todorok.messaging.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class DebeziumOutboxRoundTripTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
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
    void routesPlannerAndActivityEnvelopes() throws Exception {
        var taskEnvelope = fixtureJson("fixtures/events/task-changed/v1-valid.json");
        var activityEnvelope = fixtureJson("fixtures/events/activity-completed/v1-valid.json");
        var taskEventId = UUID.fromString(taskEnvelope.path("eventId").asText());
        var activityEventId = UUID.fromString(activityEnvelope.path("eventId").asText());

        fixture.insertOutbox(
                "planner", taskEventId, "task", "task-1",
                "TASK_CHANGED", MAPPER.writeValueAsString(taskEnvelope));
        fixture.insertOutbox(
                "activity", activityEventId, "activity", "activity-1",
                "ACTIVITY_COMPLETED", MAPPER.writeValueAsString(activityEnvelope));

        var task = fixture.consume("todorok.task.v1", taskEventId, Duration.ofSeconds(20));
        var activity = fixture.consume(
                "todorok.activity.v1", activityEventId, Duration.ofSeconds(20));
        assertThat(task.key()).isEqualTo("task-1");
        assertThat(task.value()).isEqualTo(taskEnvelope);
        assertThat(activity.key()).isEqualTo("activity-1");
        assertThat(activity.value()).isEqualTo(activityEnvelope);
    }

    @Test
    void preservesOrderForSameAggregateAndIgnoresOtherTables() throws Exception {
        var first = UUID.fromString("00000000-0000-0000-0000-000000000411");
        var second = UUID.fromString("00000000-0000-0000-0000-000000000412");
        var firstEnvelope = withEventId(
                fixtureJson("fixtures/events/task-changed/v1-valid.json"), first);
        var secondEnvelope = withEventId(firstEnvelope, second);
        fixture.insertOutbox(
                "planner", first, "task", "task-order",
                "TASK_CHANGED", MAPPER.writeValueAsString(firstEnvelope));
        fixture.insertOutbox(
                "planner", second, "task", "task-order",
                "TASK_CHANGED", MAPPER.writeValueAsString(secondEnvelope));
        fixture.updateServiceMetadata("planner", 2);

        var firstRecord = fixture.consume(
                "todorok.task.v1", first, Duration.ofSeconds(20));
        var secondRecord = fixture.consume(
                "todorok.task.v1", second, Duration.ofSeconds(20));
        assertThat(firstRecord.offset()).isLessThan(secondRecord.offset());
        assertThat(fixture.hasCdcRecordForServiceMetadata(Duration.ofSeconds(2))).isFalse();
    }

    @Test
    void publishesEventInsertedWhileConnectIsStopped() throws Exception {
        var eventId = UUID.fromString("00000000-0000-0000-0000-000000000421");
        var envelope = withEventId(
                fixtureJson("fixtures/events/task-changed/v1-valid.json"), eventId);

        fixture.stopConnect();
        fixture.insertOutbox(
                "planner", eventId, "task", "task-connect-recovery",
                "TASK_CHANGED", MAPPER.writeValueAsString(envelope));
        fixture.startConnect();

        assertThat(fixture.consume("todorok.task.v1", eventId, Duration.ofSeconds(30)).value())
                .isEqualTo(envelope);
    }

    @Test
    void publishesEventAfterKafkaRecovers() throws Exception {
        var eventId = UUID.fromString("00000000-0000-0000-0000-000000000422");
        var envelope = withEventId(
                fixtureJson("fixtures/events/activity-completed/v1-valid.json"), eventId);

        fixture.stopKafka();
        fixture.insertOutbox(
                "activity", eventId, "activity", "activity-kafka-recovery",
                "ACTIVITY_COMPLETED", MAPPER.writeValueAsString(envelope));
        fixture.startKafka();

        assertThat(fixture.consume(
                "todorok.activity.v1", eventId, Duration.ofSeconds(45)).value())
                .isEqualTo(envelope);
    }

    @Test
    void restoresRetainedOutboxAfterReplicationSlotLoss() throws Exception {
        var eventId = UUID.fromString("00000000-0000-0000-0000-000000000423");
        var envelope = withEventId(
                fixtureJson("fixtures/events/task-changed/v1-valid.json"), eventId);

        fixture.stopConnector();
        fixture.recreateReplicationSlot();
        fixture.insertOutbox(
                "planner", eventId, "task", "task-slot-recovery",
                "TASK_CHANGED", MAPPER.writeValueAsString(envelope));
        fixture.startConnector();

        assertThat(fixture.consume("todorok.task.v1", eventId, Duration.ofSeconds(45)).value())
                .isEqualTo(envelope);
    }

    private JsonNode fixtureJson(String path) throws Exception {
        try (var input = DebeziumOutboxRoundTripTest.class
                .getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("fixture not found: " + path);
            return MAPPER.readTree(input);
        }
    }

    private JsonNode withEventId(JsonNode source, UUID eventId) {
        var copy = (ObjectNode) source.deepCopy();
        copy.put("eventId", eventId.toString());
        return copy;
    }
}
