package io.todorok.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.todorok.contracts.events.ActivityCompleted;
import io.todorok.contracts.events.ActivityVoided;
import io.todorok.contracts.events.TaskChanged;
import io.todorok.contracts.events.TaskScheduled;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class EventSchemaContractTest {

    private static final String SCHEMA_ROOT = "https://todorok.app/schemas/events/";
    private static final tools.jackson.databind.ObjectMapper MAPPER =
            tools.jackson.databind.json.JsonMapper.builder().findAndAddModules().build();

    @ParameterizedTest
    @MethodSource("eventContracts")
    void validatesValidAndInvalidFixtures(String directory, String eventType) {
        var schema = schema(directory);
        var valid = resource("fixtures/events/" + directory + "/v1-valid.json");
        var invalid = resource("fixtures/events/" + directory + "/v1-invalid.json");

        assertThat(validate(schema, valid)).isEmpty();
        assertThat(validate(schema, invalid)).isNotEmpty();
        assertThat(valid).contains("\"type\": \"" + eventType + "\"");
    }

    @ParameterizedTest
    @MethodSource("serializedEvents")
    void serializedJavaRecordMatchesSchema(String directory, EventEnvelope<?> envelope) throws Exception {
        assertThat(validate(schema(directory), MAPPER.writeValueAsString(envelope))).isEmpty();
    }

    @Test
    void eventEnvelopeExposesAggregateVersionSeparatelyFromSchemaVersion() {
        var serialized = MAPPER.valueToTree(envelope(
                EventType.TASK_SCHEDULED,
                new TaskScheduled(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "GENERAL",
                        LocalDate.parse("2026-09-02"),
                        "PLANNED")));

        assertThat(serialized.path("version").asInt()).isEqualTo(1);
        assertThat(serialized.path("aggregateVersion").asLong()).isEqualTo(1L);
    }

    private static Stream<Arguments> eventContracts() {
        return Stream.of(
                Arguments.of("task-scheduled", "TASK_SCHEDULED"),
                Arguments.of("task-changed", "TASK_CHANGED"),
                Arguments.of("activity-completed", "ACTIVITY_COMPLETED"),
                Arguments.of("activity-corrected", "ACTIVITY_CORRECTED"),
                Arguments.of("activity-voided", "ACTIVITY_VOIDED"));
    }

    private static Stream<Arguments> serializedEvents() {
        var taskId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        var activityId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        return Stream.of(
                Arguments.of("activity-sync-result", envelope(EventType.ACTIVITY_SYNC_RESULT,
                        new io.todorok.contracts.events.ActivitySyncResult(activityId, taskId, "CONFLICT", "TASK_DELETED"))),
                Arguments.of("activity-corrected", envelope(EventType.ACTIVITY_CORRECTED,
                        new io.todorok.contracts.events.ActivityCorrected(activityId, taskId, "STUDY", Instant.parse("2026-09-02T00:00:00Z"), "공부 기록", null, null, Instant.parse("2026-09-01T00:00:00Z"), "COMPLETED"))),
                Arguments.of("task-scheduled", envelope(
                        EventType.TASK_SCHEDULED,
                        new TaskScheduled(taskId, "WORKOUT", LocalDate.parse("2026-09-02"), "PLANNED"))),
                Arguments.of("task-changed", envelope(EventType.TASK_CHANGED,
                        new TaskChanged(taskId,"WORKOUT",LocalDate.parse("2026-09-02"),"SKIPPED","SKIPPED"))),
                Arguments.of("task-rolled-over", envelope(EventType.TASK_ROLLED_OVER,
                        new TaskChanged(taskId,"WORKOUT",LocalDate.parse("2026-09-09"),"PLANNED","ROLLED_OVER"))),
                Arguments.of("series-changed", envelope(EventType.SERIES_CHANGED,
                        Map.of("seriesId",taskId,"command","ARCHIVED"))),
                Arguments.of("task-changed", envelope(
                        EventType.TASK_CHANGED,
                        new TaskChanged(
                                taskId,
                                "WORKOUT",
                                LocalDate.parse("2026-09-02"),
                                "COMPLETED",
                                "COMPLETED"))),
                Arguments.of("activity-completed", envelope(
                        EventType.ACTIVITY_COMPLETED,
                        new ActivityCompleted(
                                activityId,
                                taskId,
                                "WORKOUT",
                                Instant.parse("2026-09-02T00:00:00Z"),
                                "5세트 완료"))),
                Arguments.of("activity-voided", envelope(
                        EventType.ACTIVITY_VOIDED,
                        new ActivityVoided(
                                activityId,
                                taskId,
                                Instant.parse("2026-09-02T00:05:00Z"),
                                "중복 기록"))));
    }

    private static EventEnvelope<?> envelope(EventType type, Object payload) {
        return new EventEnvelope<>(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                type,
                1,
                1,
                Instant.parse("2026-09-02T00:00:00Z"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                payload);
    }

    private Schema schema(String directory) {
        var schemas = Map.of(
                SCHEMA_ROOT + "envelope/v1", resource("events/envelope/v1.schema.json"),
                SCHEMA_ROOT + directory + "/v1", resource("events/" + directory + "/v1.schema.json"));
        var registry = SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_2020_12,
                builder -> builder.schemas(schemas));
        return registry.getSchema(SchemaLocation.of(SCHEMA_ROOT + directory + "/v1"));
    }

    private java.util.List<com.networknt.schema.Error> validate(Schema schema, String input) {
        return schema.validate(
                input,
                InputFormat.JSON,
                context -> context.executionConfig(config -> config.formatAssertionsEnabled(true)));
    }

    private static String resource(String path) {
        try (var input = EventSchemaContractTest.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("계약 리소스를 찾을 수 없습니다: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
