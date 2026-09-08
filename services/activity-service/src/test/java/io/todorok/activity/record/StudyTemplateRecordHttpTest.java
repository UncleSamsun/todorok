package io.todorok.activity.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import io.todorok.activity.template.TemplateBindingService;
import io.todorok.internal.api.model.TemplateSelectionRequest;
import io.todorok.web.ApiFailure;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest(classes = io.todorok.activity.ActivityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.flyway.enabled=true", "spring.flyway.create-schemas=false",
    "spring.flyway.locations=classpath:db/migration", "spring.jpa.hibernate.ddl-auto=none",
    "todorok.messaging.enabled=false"
})
class StudyTemplateRecordHttpTest {
    static final KeyPair KEYS = keys();
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.11-alpine")
        .withDatabaseName("todorok").withUsername("postgres").withPassword("admin-password")
        .withEnv("PLANNER_DB_PASSWORD", "planner-test-password").withEnv("ACTIVITY_DB_PASSWORD", "activity-test-password")
        .withEnv("NOTIFICATION_DB_PASSWORD", "notification-test-password").withEnv("DEBEZIUM_DB_PASSWORD", "debezium-test-password")
        .withCopyFileToContainer(MountableFile.forHostPath(Path.of(System.getProperty("todorok.repository.root"))
            .resolve("infra/docker/postgres/init/001-create-service-roles.sh")), "/docker-entrypoint-initdb.d/001-create-service-roles.sh");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl() + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=activity");
        registry.add("spring.datasource.username", () -> "activity_app");
        registry.add("spring.datasource.password", () -> "activity-test-password");
        registry.add("todorok.auth.public-key", () -> Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded()));
    }
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired TemplateBindingService bindings;
    @Autowired ActivityEventConsumer consumer;
    @Autowired ActivityService activities;
    @Autowired ActivityDetailStore details;
    @Autowired io.todorok.messaging.OutboxEventWriter outbox;
    @Autowired io.todorok.activity.template.TemplateService templateService;
    @Autowired io.todorok.activity.program.ProgramCatalogStore catalogs;
    @Autowired io.todorok.activity.program.ProgramEnrollmentService enrollments;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;

    @Test void importsSyntheticCatalogIdempotentlyAndRejectsChecksumReplacement() throws Exception {
        var source = mapper.readTree(java.nio.file.Files.readString(Path.of(System.getProperty("todorok.repository.root"), "contracts", "fixtures", "catalog", "program-v1-valid.json")));
        assertThat(catalogs.importCatalog(source).result()).isEqualTo(io.todorok.activity.program.ProgramCatalogStore.ImportResult.IMPORTED);
        assertThat(catalogs.importCatalog(source).result()).isEqualTo(io.todorok.activity.program.ProgramCatalogStore.ImportResult.UNCHANGED);
        assertThat(jdbc.queryForObject("select count(*) from program_catalog where catalog_key='synthetic-pushup' and catalog_version=1", Integer.class)).isOne();
        ((tools.jackson.databind.node.ObjectNode) source).put("name", "바뀐 합성 프로그램");
        ((tools.jackson.databind.node.ObjectNode) source).put("checksum", new io.todorok.activity.program.ProgramCatalogImporter(mapper).checksum(source));
        assertThatThrownBy(() -> catalogs.importCatalog(source)).isInstanceOf(ApiFailure.class)
            .extracting(error -> ((ApiFailure) error).code()).isEqualTo("CATALOG_VERSION_CONFLICT");
        assertThat(jdbc.queryForObject("select name from program_catalog where catalog_key='synthetic-pushup' and catalog_version=1", String.class)).isEqualTo("합성 푸시업 프로그램");
    }

    @Test void enrollsAgainstThePinnedCatalogWithOneIdempotentFirstSession() throws Exception {
        var catalog = mapper.readTree(java.nio.file.Files.readString(Path.of(System.getProperty("todorok.repository.root"), "contracts", "fixtures", "catalog", "program-v1-valid.json")));
        catalogs.importCatalog(catalog);
        UUID owner = UUID.randomUUID(), command = UUID.randomUUID();
        var enrolled = enrollments.enroll(owner, command, "synthetic-pushup", 1, 12, 2);
        assertThat(enrolled.recommendedWeek()).isOne();
        assertThat(enrolled.startWeek()).isEqualTo(2);
        assertThat(enrolled.currentWeek()).isEqualTo(2);
        assertThat(enrolled.currentSession()).isOne();
        assertThat(enrolled.targetSets()).containsExactly(4, 3, 3);
        assertThat(enrollments.enroll(owner, command, "synthetic-pushup", 1, 12, 2)).isEqualTo(enrolled);
        assertThat(jdbc.queryForObject("select count(*) from program_session where enrollment_id=?", Integer.class, enrolled.id())).isOne();
        assertThatThrownBy(() -> enrollments.enroll(owner, command, "synthetic-pushup", 1, 13, 2)).isInstanceOf(ApiFailure.class)
            .extracting(error -> ((ApiFailure) error).code()).isEqualTo("COMMAND_CONFLICT");
    }

    @Test void exposesImportedCatalogAndEnrollmentOverOwnerScopedHttp() throws Exception {
        var catalog = mapper.readTree(java.nio.file.Files.readString(Path.of(System.getProperty("todorok.repository.root"), "contracts", "fixtures", "catalog", "program-v1-valid.json")));
        catalogs.importCatalog(catalog);
        UUID owner = UUID.randomUUID(), command = UUID.randomUUID();
        var programs = ok(send("GET", "/programs", null, owner), 200);
        assertThat(programs.get(0).path("catalogKey").asText()).isEqualTo("synthetic-pushup");
        var body = Map.of("commandId", command, "catalogKey", "synthetic-pushup", "catalogVersion", 1, "initialTestValue", 9, "startWeek", 2);
        var enrolled = ok(send("POST", "/program-enrollments", body, owner), 201);
        assertThat(enrolled.path("startWeek").asInt()).isEqualTo(2);
        assertThat(enrolled.path("target").path("targetSets")).isEqualTo(mapper.readTree("[4,3,3]"));
        assertThat(ok(send("POST", "/program-enrollments", body, owner), 201)).isEqualTo(enrolled);
        assertThat(ok(send("GET", "/program-enrollments/" + enrolled.path("enrollmentId").asText(), null, owner), 200)).isEqualTo(enrolled);
        assertThat(send("GET", "/program-enrollments/" + enrolled.path("enrollmentId").asText(), null, UUID.randomUUID()).statusCode()).isEqualTo(404);
    }

    @Test void workoutTemplateKeepsRelationalSetsAndCustomValuesAcrossCorrection() throws Exception {
        UUID owner = UUID.randomUUID(), task = UUID.randomUUID();
        var numberId = UUID.randomUUID();
        var checkId = UUID.randomUUID();
        var fields = List.of(
            Map.<String,Object>of("fieldId", numberId, "type", "NUMBER", "name", "RPE", "unit", "점"),
            Map.<String,Object>of("fieldId", checkId, "type", "CHECK", "name", "통증 없음")
        );
        var template = ok(send("POST", "/templates", Map.of("commandId", UUID.randomUUID(), "name", "자유 운동",
            "domain", "WORKOUT", "kind", "FREE_WORKOUT", "fields", fields), owner), 201).path("templateId").asText();
        var binding = bindings.approve(new TemplateSelectionRequest(UUID.randomUUID(), owner,
            TemplateSelectionRequest.TargetTypeEnum.TASK, task, TemplateSelectionRequest.TaskTypeEnum.WORKOUT, UUID.fromString(template), 1L));
        var payload = new LinkedHashMap<String,Object>(Map.of("taskId", task, "taskType", "WORKOUT", "status", "PLANNED",
            "scheduledDate", "2026-09-07", "templateLink", Map.of("bindingId", binding.getBindingId(), "templateId", template, "selectedTemplateVersion", 1)));
        payload.put("seriesId", null);
        consumer.receive(mapper.writeValueAsString(Map.of("eventId", UUID.randomUUID(), "type", "TASK_SCHEDULED", "version", 2,
            "aggregateVersion", 0, "occurredAt", Instant.now().toString(), "userId", owner, "payload", payload)));

        var detail = Map.of("workout", Map.of(
            "sets", List.of(Map.of("exercise", "스쿼트", "reps", 8, "weightKg", 80, "durationSeconds", 40)),
            "fields", List.of(Map.of("fieldId", numberId, "type", "NUMBER", "numberValue", 7.5),
                Map.of("fieldId", checkId, "type", "CHECK", "checked", false))));
        var request = new LinkedHashMap<String,Object>(Map.of("commandId", UUID.randomUUID(), "taskId", task, "activityType", "WORKOUT",
            "completionStatus", "COMPLETED", "performedAt", "2026-09-07T10:00:00+09:00", "expectedTemplateVersion", 1, "detail", detail));
        var saved = ok(send("POST", "/activities", request, owner), 201);
        UUID activityId = UUID.fromString(saved.path("activityId").asText());
        assertThat(saved.path("detail").path("workout").path("sets").get(0).path("weightKg").decimalValue()).isEqualByComparingTo("80");
        assertThat(saved.path("detail").path("workout").path("fields").size()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from activity_field_value where activity_id=?", Integer.class, activityId)).isEqualTo(2);
        assertThatThrownBy(() -> jdbc.update("update activity_field_value set type='TIME',number_value=null,time_seconds=1 where activity_id=? and field_id=?", activityId, numberId))
            .as("The relational FK must prevent a stored value from changing the definition type")
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("select type from activity_field_value where activity_id=? and field_id=?", String.class, activityId, numberId)).isEqualTo("NUMBER");

        var correctedDetail = Map.of("workout", Map.of(
            "sets", List.of(Map.of("exercise", "스쿼트", "reps", 10, "weightKg", 82.5)),
            "fields", List.of(Map.of("fieldId", checkId, "type", "CHECK", "checked", true))));
        var corrected = ok(send("PATCH", "/activities/" + activityId, Map.of("expectedVersion", 0,
            "performedAt", "2026-09-07T10:00:00+09:00", "detail", correctedDetail), owner), 200);
        assertThat(corrected.path("templateSnapshot")).isEqualTo(saved.path("templateSnapshot"));
        assertThat(corrected.path("detail").path("workout").path("sets").get(0).path("reps").asInt()).isEqualTo(10);
        assertThat(corrected.path("detail").path("workout").path("fields").size()).isOne();
        assertThat(jdbc.queryForObject("select checked from activity_field_value where activity_id=?", Boolean.class, activityId)).isTrue();
        assertThat(mapper.readTree(jdbc.queryForObject("select snapshot::text from activity_revision_history where activity_id=? and revision=0", String.class, activityId)))
            .isEqualTo(saved);
    }

    @Test void climbingTemplateKeepsRoundsAndDoesNotDoubleCountCustomTime() throws Exception {
        UUID owner = UUID.randomUUID(), task = UUID.randomUUID();
        var timeId = UUID.randomUUID();
        var memoId = UUID.randomUUID();
        var fields = List.of(
            Map.<String,Object>of("fieldId", timeId, "type", "TIME", "name", "휴식 시간", "unit", "분"),
            Map.<String,Object>of("fieldId", memoId, "type", "MEMO", "name", "느낌")
        );
        var template = ok(send("POST", "/templates", Map.of("commandId", UUID.randomUUID(), "name", "클라이밍 세션",
            "domain", "CLIMBING", "kind", "CLIMBING_SESSION", "fields", fields), owner), 201).path("templateId").asText();
        var binding = bindings.approve(new TemplateSelectionRequest(UUID.randomUUID(), owner,
            TemplateSelectionRequest.TargetTypeEnum.TASK, task, TemplateSelectionRequest.TaskTypeEnum.CLIMBING, UUID.fromString(template), 1L));
        var payload = new LinkedHashMap<String,Object>(Map.of("taskId", task, "taskType", "CLIMBING", "status", "PLANNED",
            "scheduledDate", "2026-09-07", "templateLink", Map.of("bindingId", binding.getBindingId(), "templateId", template, "selectedTemplateVersion", 1)));
        payload.put("seriesId", null);
        consumer.receive(mapper.writeValueAsString(Map.of("eventId", UUID.randomUUID(), "type", "TASK_SCHEDULED", "version", 2,
            "aggregateVersion", 0, "occurredAt", Instant.now().toString(), "userId", owner, "payload", payload)));

        var detail = Map.of("climbing", Map.of("durationSeconds", 600,
            "rounds", List.of(Map.of("grade", "V5", "attempts", 2, "completed", false)),
            "fields", List.of(Map.of("fieldId", timeId, "type", "TIME", "timeSeconds", 3600),
                Map.of("fieldId", memoId, "type", "MEMO", "memoValue", "슬로퍼"))));
        var request = new LinkedHashMap<String,Object>(Map.of("commandId", UUID.randomUUID(), "taskId", task, "activityType", "CLIMBING",
            "completionStatus", "COMPLETED", "performedAt", "2026-09-07T11:00:00+09:00", "expectedTemplateVersion", 1, "detail", detail));
        var saved = ok(send("POST", "/activities", request, owner), 201);
        UUID activityId = UUID.fromString(saved.path("activityId").asText());
        assertThat(saved.path("detail").path("climbing").path("rounds").get(0).path("completed").asBoolean()).isFalse();
        assertThat(saved.path("detail").path("climbing").path("fields").size()).isEqualTo(2);
        assertThat(activities.monthlySummary(owner, java.time.YearMonth.of(2026, 9), io.todorok.activity.api.model.ActivityType.CLIMBING).getDurationSeconds()).isEqualTo(600);

        var correctedDetail = Map.of("climbing", Map.of("durationSeconds", 900,
            "rounds", List.of(Map.of("grade", "V5", "attempts", 3, "completed", true)),
            "fields", List.of(Map.of("fieldId", timeId, "type", "TIME", "timeSeconds", 7200))));
        var corrected = ok(send("PATCH", "/activities/" + activityId, Map.of("expectedVersion", 0,
            "performedAt", "2026-09-07T11:00:00+09:00", "detail", correctedDetail), owner), 200);
        assertThat(corrected.path("detail").path("climbing").path("rounds").get(0).path("completed").asBoolean()).isTrue();
        assertThat(corrected.path("detail").path("climbing").path("fields").size()).isOne();
        assertThat(jdbc.queryForObject("select time_seconds from activity_field_value where activity_id=?", Long.class, activityId)).isEqualTo(7200);
        assertThat(activities.monthlySummary(owner, java.time.YearMonth.of(2026, 9), io.todorok.activity.api.model.ActivityType.CLIMBING).getDurationSeconds()).isEqualTo(900);
        assertThat(mapper.readTree(jdbc.queryForObject("select snapshot::text from activity_revision_history where activity_id=? and revision=0", String.class, activityId)))
            .isEqualTo(saved);
        var voided = ok(send("POST", "/activities/" + activityId + "/void", Map.of("reason", "손가락 휴식", "version", 1), owner), 200);
        assertThat(voided.path("status").asText()).isEqualTo("VOIDED");
        assertThat(voided.path("detail").path("climbing").path("fields").size()).isOne();
        assertThat(jdbc.queryForObject("select count(*) from activity_field_value where activity_id=?", Integer.class, activityId)).isOne();
        assertThat(jdbc.queryForObject("select count(*) from activity_revision_history where activity_id=?", Integer.class, activityId)).isEqualTo(2);
        assertThat(activities.monthlySummary(owner, java.time.YearMonth.of(2026, 9), io.todorok.activity.api.model.ActivityType.CLIMBING).getDurationSeconds()).isZero();
    }

    @Test void fiveTypesRoundTripWithOriginalDefinitionAfterEditArchiveCorrectionAndVoid() throws Exception {
        UUID owner = UUID.randomUUID(), task = UUID.randomUUID();
        var definition = definition();
        String template = setup(owner, task, definition);
        var fields = inputs(definition);
        var request = create(task, fields);
        request.put("startedAt", "2026-09-07T10:00:00.123456+09:00");
        request.put("endedAt", "2026-09-07T10:30:00.987654+09:00");
        JsonNode saved = ok(send("POST", "/activities", request, owner), 201);
        String id = saved.path("activityId").asText();
        assertThat(saved.path("detailFormat").asText()).isEqualTo("TEMPLATE");
        assertThat(saved.path("templateSnapshot").path("name").asText()).isEqualTo("알고리즘");
        assertThat(saved.path("templateSnapshot").path("fields").size()).isEqualTo(6);
        assertThat(saved.path("detail").path("study").path("fields").size()).isEqualTo(5);
        assertThat(saved.path("detail").path("study").path("fields").get(0).path("numberValue").asInt()).isZero();
        assertThat(saved.path("detail").path("study").path("fields").get(3).path("checked").asBoolean(true)).isFalse();
        assertThat(mapper.readTree(jdbc.queryForObject("select values_json::text from study_detail where activity_id=?", String.class, UUID.fromString(id))))
            .isEqualTo(mapper.valueToTree(Map.of(definition.get(0).get("fieldId").toString(), 0,
                definition.get(1).get("fieldId").toString(), 0, definition.get(2).get("fieldId").toString(), "BFS",
                definition.get(3).get("fieldId").toString(), false, definition.get(4).get("fieldId").toString(), " 첫 줄\n둘째 줄 ")));
        ok(send("POST", "/templates/" + template + "/versions", Map.of("commandId", UUID.randomUUID(),
            "expectedRevision", 0, "name", "새 정의", "fields", List.of()), owner), 201);
        ok(send("POST", "/templates/" + template + "/archive", Map.of("commandId", UUID.randomUUID(), "expectedRevision", 1), owner), 200);
        assertThat(ok(send("POST", "/activities", request, owner), 201)).isEqualTo(saved);
        var correction = Map.of("expectedVersion", 0, "performedAt", "2026-09-07T10:00:00+09:00",
            "startedAt", saved.path("startedAt").asText(), "endedAt", saved.path("endedAt").asText(),
            "detail", Map.of("study", Map.of("fields", List.of(fields.get(3)))));
        var corrected = ok(send("PATCH", "/activities/" + id, correction, owner), 200);
        assertThat(corrected.path("templateSnapshot")).isEqualTo(saved.path("templateSnapshot"));
        assertThat(corrected.path("startedAt")).isEqualTo(saved.path("startedAt"));
        assertThat(corrected.path("endedAt")).isEqualTo(saved.path("endedAt"));
        assertThat(corrected.path("detail").path("study").path("fields").size()).isOne();
        assertThat(ok(send("GET", "/activities/" + id, null, owner), 200)).isEqualTo(corrected);
        assertThat(ok(send("GET", "/activities", null, owner), 200).path("items").get(0)).isEqualTo(corrected);
        assertThat(ok(send("POST", "/activities", request, owner), 201)).isEqualTo(corrected);
        var history = mapper.readTree(jdbc.queryForObject("select snapshot::text from activity_revision_history where activity_id=? and revision=0", String.class, UUID.fromString(id)));
        assertThat(history).isEqualTo(saved);
        assertThat(ok(send("POST", "/activities/" + id + "/void", Map.of("reason", "취소", "version", 1), owner), 200).path("templateSnapshot"))
            .isEqualTo(saved.path("templateSnapshot"));
        assertThat(jdbc.queryForObject("select count(*) from activity_revision_history where activity_id=?", Integer.class, UUID.fromString(id))).isEqualTo(2);
    }

    @Test void rejectsInvalidInputsAndStaleVersionsWithoutRecordsOrOutbox() throws Exception {
        UUID owner = UUID.randomUUID(), task = UUID.randomUUID();
        var definition = definition();
        String template = setup(owner, task, definition);
        String number = definition.get(0).get("fieldId").toString(), time = definition.get(1).get("fieldId").toString();
        String text = definition.get(2).get("fieldId").toString(), check = definition.get(3).get("fieldId").toString();
        var invalid = List.of(
            "{\"fieldId\":\"" + number + "\",\"type\":\"NUMBER\",\"numberValue\":null}",
            "{\"fieldId\":\"" + number + "\",\"type\":\"NUMBER\",\"numberValue\":\"1\"}",
            "{\"fieldId\":\"" + number + "\",\"type\":\"NUMBER\",\"numberValue\":0,\"checked\":false}",
            "{\"fieldId\":\"" + number + "\",\"type\":\"NUMBER\",\"numberValue\":0,\"numberValue\":1}",
            "{\"fieldId\":\"" + number + "\",\"type\":\"NUMBER\",\"numberValue\":0,\"unknown\":1}",
            "{\"fieldId\":\"" + number + "\",\"type\":\"NUMBER\",\"numberValue\":1e400}",
            "{\"fieldId\":\"" + time + "\",\"type\":\"TIME\",\"timeSeconds\":-1}",
            "{\"fieldId\":\"" + time + "\",\"type\":\"TIME\",\"timeSeconds\":0.5}",
            "{\"fieldId\":\"" + time + "\",\"type\":\"TIME\",\"timeSeconds\":9007199254740992}",
            "{\"fieldId\":\"" + text + "\",\"type\":\"SHORT_TEXT\",\"textValue\":123}",
            "{\"fieldId\":\"" + check + "\",\"type\":\"CHECK\",\"checked\":\"false\"}",
            "{\"fieldId\":\"" + number + "\",\"type\":\"CHECK\",\"checked\":false}",
            "{\"fieldId\":\"" + UUID.randomUUID() + "\",\"type\":\"NUMBER\",\"numberValue\":0}",
            "null", "{}");
        for (String field : invalid) {
            String body = mapper.writeValueAsString(create(task, List.of())).replace("\"fields\":[]", "\"fields\":[" + field + "]");
            ok(send("POST", "/activities", body, owner), 400);
        }
        var input = inputs(definition).getFirst();
        assertThat(ok(send("POST", "/activities", create(task, List.of(input, input)), owner), 400).path("code").asText()).isEqualTo("FIELD_ID_DUPLICATE");
        var request = create(task, List.of());
        request.remove("expectedTemplateVersion");
        ok(send("POST", "/activities", request, owner), 400);
        request.put("templateSnapshot", Map.of("name", "forged"));
        ok(send("POST", "/activities", request, owner), 400);
        ok(send("POST", "/templates/" + template + "/versions", Map.of("commandId", UUID.randomUUID(), "expectedRevision", 0,
            "name", "새 정의", "fields", definition), owner), 201);
        assertThat(ok(send("POST", "/activities", create(task, List.of()), owner), 409).path("code").asText()).isEqualTo("TEMPLATE_VERSION_CONFLICT");
        assertThat(jdbc.queryForObject("select count(*) from activity_record where user_id=?", Integer.class, owner)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where payload->>'userId'=?", Integer.class, owner.toString())).isZero();
        var current = create(task, List.of()); current.put("expectedTemplateVersion", 2);
        ok(send("POST", "/templates/" + template + "/archive", Map.of("commandId", UUID.randomUUID(), "expectedRevision", 1), owner), 200);
        assertThat(ok(send("POST", "/activities", current, owner), 201).path("templateSnapshot").path("templateVersion").asInt()).isEqualTo(2);
    }

    @Test void standardRejectsLegacyWritesAndUnexpectedTemplateVersion() throws Exception {
        UUID owner = UUID.randomUUID(), task = UUID.randomUUID();
        jdbc.update("insert into task_reference(task_id,user_id,task_type,scheduled_date,status,version) values (?,?,'STUDY',date '2026-09-07','PLANNED',0)", task, owner);
        var request = create(task, List.of());
        ok(send("POST", "/activities", request, owner), 400);
        request.remove("expectedTemplateVersion");
        for (String member : List.of("values", "snapshot", "legacyStudyPayload")) {
            request.put("detail", Map.of("study", Map.of(member, Map.of("nested", List.of(0, false)))));
            ok(send("POST", "/activities", request, owner), 400);
        }
        request.put("detail", Map.of("study", Map.of("subject", "기본 공부", "durationMinutes", 30)));
        assertThat(ok(send("POST", "/activities", request, owner), 201).path("detailFormat").asText()).isEqualTo("STANDARD");
    }

    @Test void migrationAndLegacyCorrectionPreserveArbitraryJsonAndHistory() throws Exception {
        String values = "{\"nested\":{\"a\":[null,0,false,{}]},\"陌生\":[],\"empty\":{},\"zero\":0,\"false\":false,\"decimal\":123456789.123456789123456789}";
        String snapshot = "{\"untrusted\":[{\"type\":42}],\"x\":null}";
        UUID owner = UUID.randomUUID(), task = UUID.randomUUID(), id = UUID.randomUUID();
        var source = new org.springframework.jdbc.datasource.DriverManagerDataSource(POSTGRES.getJdbcUrl(), "postgres", "admin-password");
        var admin = new JdbcTemplate(source);
        String schema = "legacy_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("create schema " + schema);
        var flyway = org.flywaydb.core.Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema)
            .locations("classpath:db/migration").target("6").load();
        flyway.migrate();
        String header = "insert into %s.activity_record(id,user_id,task_id,activity_type,performed_at,status,command_id,fingerprint,note,sync_state) values (?,?,?,'STUDY',timestamptz '2026-09-07T01:00:00Z','PARTIAL',?,'old','old','NOT_REQUIRED')";
        admin.update(header.formatted(schema), id, owner, task, UUID.randomUUID());
        admin.update("insert into " + schema + ".study_detail values (?,'기존 공부',10,cast(? as jsonb),cast(? as jsonb))", id, values, snapshot);
        admin.update("insert into " + schema + ".activity_revision_history(activity_id,revision,snapshot) values (?,9,cast(? as jsonb))", id, snapshot);
        String before = admin.queryForObject("select row_to_json(s)::text from " + schema + ".study_detail s", String.class);
        org.flywaydb.core.Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").load().migrate();
        assertThat(admin.queryForObject("select row_to_json(s)::text from " + schema + ".study_detail s", String.class)).isEqualTo(before);
        assertThat(admin.queryForObject("select detail_format from " + schema + ".activity_record", String.class)).isEqualTo("LEGACY");
        assertThat(mapper.readTree(admin.queryForObject("select snapshot::text from " + schema + ".activity_revision_history", String.class))).isEqualTo(mapper.readTree(snapshot));

        // Copy the migrated legacy row into the actual HTTP service schema without interpreting its old JSON.
        admin.update("insert into activity.activity_record select * from " + schema + ".activity_record");
        admin.update("insert into activity.study_detail select * from " + schema + ".study_detail");
        var old = ok(send("GET", "/activities/" + id, null, owner), 200);
        assertThat(old.path("detailFormat").asText()).isEqualTo("LEGACY");
        assertThat(old.path("legacyStudyPayload").path("values")).isEqualTo(mapper.readTree(values));
        assertThat(old.path("legacyStudyPayload").path("snapshot")).isEqualTo(mapper.readTree(snapshot));
        var corrected = ok(send("PATCH", "/activities/" + id, Map.of("expectedVersion", 0, "performedAt", "2026-09-07T01:00:00Z",
            "detail", Map.of("study", Map.of("subject", "수정 과목", "durationMinutes", 20))), owner), 200);
        assertThat(corrected.path("legacyStudyPayload")).isEqualTo(old.path("legacyStudyPayload"));
        assertThat(corrected.path("detail").path("study").path("durationMinutes").asInt()).isEqualTo(20);
        assertThat(mapper.readTree(jdbc.queryForObject("select values_json::text from study_detail where activity_id=?", String.class, id))).isEqualTo(mapper.readTree(values));
        assertThat(mapper.readTree(jdbc.queryForObject("select snapshot::text from activity_revision_history where activity_id=? and revision=0", String.class, id))).isEqualTo(old);
        assertThat(jdbc.queryForObject("select h.snapshot->'legacyStudyPayload'->'values'=s.values_json from activity_revision_history h join study_detail s on s.activity_id=h.activity_id where h.activity_id=? and h.revision=0", Boolean.class, id))
            .as("Archived history must preserve the exact original JSONB numeric value").isTrue();
    }

    @Test void currentTemplateLockOrdersCreationAndDefinitionChangesInBothDirections() throws Exception {
        for (boolean versionFirst : List.of(false, true)) {
            UUID owner = UUID.randomUUID(), task = UUID.randomUUID();
            var definition = definition();
            UUID template = UUID.fromString(setup(owner, task, definition));
            var request = create(task, inputs(definition));
            var versionRequest = mapper.convertValue(Map.of("commandId", UUID.randomUUID(), "expectedRevision", 0,
                "name", "변경 후", "fields", definition), io.todorok.activity.api.model.CreateTemplateVersionRequest.class);
            var held = new java.util.concurrent.CountDownLatch(1);
            var release = new java.util.concurrent.CountDownLatch(1);
            try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
                var first = pool.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status -> {
                    if (versionFirst) templateService.createVersion(owner, template, versionRequest);
                    else activities.create(owner, mapper.convertValue(request, io.todorok.activity.api.model.CreateActivityRequest.class));
                    held.countDown();
                    try { if (!release.await(20, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Lock release timed out"); }
                    catch (InterruptedException e) { throw new IllegalStateException(e); }
                    return true;
                }));
                assertThat(held.await(20, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                var second = pool.submit(() -> versionFirst ? send("POST", "/activities", request, owner)
                    : send("POST", "/templates/" + template + "/versions", versionRequest, owner));
                try {
                    long until = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
                    boolean blocked = false;
                    while (System.nanoTime() < until) {
                        blocked = jdbc.queryForObject("select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and query like '%record_template%for update%'", Integer.class) > 0;
                        if (blocked) break;
                        Thread.sleep(25);
                    }
                    assertThat(blocked).as("Second operation must wait on the same template identity lock").isTrue();
                } finally { release.countDown(); }
                first.get(20, java.util.concurrent.TimeUnit.SECONDS);
                ok(second.get(20, java.util.concurrent.TimeUnit.SECONDS), versionFirst ? 409 : 201);
            } finally { release.countDown(); }
            assertThat(jdbc.queryForObject("select count(*) from activity_record where user_id=?", Integer.class, owner)).isEqualTo(versionFirst ? 0 : 1);
            if (!versionFirst) assertThat(jdbc.queryForObject("select template_version from activity_record where user_id=?", Long.class, owner)).isEqualTo(1);
        }
    }

    @Test void readsTemplateAndValuesFromOneRevisionDuringCorrection() throws Exception {
        for (String operation : List.of("GET", "LIST", "REPLAY")) {
            UUID owner = UUID.randomUUID(), task = UUID.randomUUID();
            var definition = definition();
            setup(owner, task, definition);
            var request = mapper.convertValue(create(task, inputs(definition)), io.todorok.activity.api.model.CreateActivityRequest.class);
            var original = activities.create(owner, request);
            var obtained = new java.util.concurrent.CountDownLatch(1);
            var resume = new java.util.concurrent.CountDownLatch(1);
            var armed = new java.util.concurrent.atomic.AtomicBoolean(true);
            var barrierJdbc = new JdbcTemplate(jdbc.getDataSource()) {
                @Override public <T> List<T> query(String sql, org.springframework.jdbc.core.RowMapper<T> rowMapper, Object... args) {
                    return super.query(sql, (rs, index) -> {
                        if (sql.contains("detail_snapshot from activity_record") && armed.compareAndSet(true, false)) {
                            obtained.countDown();
                            try { if (!resume.await(20, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Reader barrier timed out"); }
                            catch (InterruptedException e) { throw new IllegalStateException(e); }
                        }
                        return rowMapper.mapRow(rs, index);
                    }, args);
                }
            };
            var reader = new ActivityService(barrierJdbc, details, outbox, mapper, java.time.Clock.systemUTC());
            try (var pool = java.util.concurrent.Executors.newSingleThreadExecutor()) {
                var pending = pool.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status -> switch (operation) {
                    case "GET" -> reader.get(owner, original.getActivityId());
                    case "LIST" -> reader.list(owner, null, null, 20).getItems().getFirst();
                    default -> reader.create(owner, request);
                }));
                try {
                    assertThat(obtained.await(20, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                    activities.correct(owner, original.getActivityId(), new io.todorok.activity.api.model.CorrectActivityRequest(0L,
                        request.getPerformedAt(), new io.todorok.activity.api.model.ActivityDetail().study(new io.todorok.activity.api.model.StudyDetail().fields(List.of()))).note("수정"));
                } finally { resume.countDown(); }
                assertThat(pending.get(20, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(original);
                assertThat(activities.get(owner, original.getActivityId()).getDetail().getStudy().getFields()).isEmpty();
                assertThat(activities.get(owner, original.getActivityId()).getTemplateSnapshot()).isEqualTo(original.getTemplateSnapshot());
            }
        }
    }

    @Test void optionalTextAndTimeDoNotCreatePhantomValuesOrDoubleCountDurations() throws Exception {
        UUID owner = UUID.randomUUID(), task = UUID.randomUUID();
        var definition = definition();
        setup(owner, task, definition);
        var values = new ArrayList<>(inputs(definition));
        values.set(1, Map.of("fieldId", definition.get(1).get("fieldId"), "type", "TIME", "timeSeconds", 9007199254740991L));
        values.set(2, Map.of("fieldId", definition.get(2).get("fieldId"), "type", "SHORT_TEXT", "textValue", " \n\u00a0"));
        values.set(4, Map.of("fieldId", definition.get(4).get("fieldId"), "type", "MEMO", "memoValue", " \n "));
        var request = create(task, values);
        var saved = ok(send("POST", "/activities", request, owner), 201);
        assertThat(saved.path("detail").path("study").path("fields").size()).isEqualTo(3);
        assertThat(activities.monthlySummary(owner, java.time.YearMonth.of(2026,9), io.todorok.activity.api.model.ActivityType.STUDY).getDurationSeconds()).isZero();
        String id = saved.path("activityId").asText();
        var correction = new LinkedHashMap<String,Object>(Map.of("expectedVersion", 0, "performedAt", "2026-09-07T10:00:00+09:00",
            "startedAt", "2026-09-07T10:00:00+09:00", "endedAt", "2026-09-07T10:30:00+09:00",
            "detail", Map.of("study", Map.of("fields", values, "durationMinutes", 99))));
        ok(send("PATCH", "/activities/" + id, correction, owner), 200);
        assertThat(activities.monthlySummary(owner, java.time.YearMonth.of(2026,9), io.todorok.activity.api.model.ActivityType.STUDY).getDurationSeconds()).isEqualTo(1800);
        correction.put("expectedVersion", 1); correction.remove("startedAt"); correction.remove("endedAt");
        assertThat(ok(send("PATCH", "/activities/" + id, correction, owner), 200).hasNonNull("startedAt")).isFalse();
        assertThat(activities.monthlySummary(owner, java.time.YearMonth.of(2026,9), io.todorok.activity.api.model.ActivityType.STUDY).getDurationSeconds()).isEqualTo(5940);
        ok(send("POST", "/activities/" + id + "/void", Map.of("reason", "취소", "version", 2), owner), 200);
        assertThat(activities.monthlySummary(owner, java.time.YearMonth.of(2026,9), io.todorok.activity.api.model.ActivityType.STUDY).getDurationSeconds()).isZero();
        assertThat(ok(send("POST", "/activities", " ".repeat(1_048_577), owner), 413).path("code").asText()).isEqualTo("PAYLOAD_TOO_LARGE");
    }

    List<Map<String,Object>> definition() {
        var fields = new ArrayList<Map<String,Object>>();
        for (String type : List.of("NUMBER", "TIME", "SHORT_TEXT", "CHECK", "MEMO", "NUMBER")) {
            var field = new LinkedHashMap<String,Object>(Map.of("fieldId", UUID.randomUUID(), "type", type, "name", type));
            if (type.equals("TIME")) field.put("unit", "분");
            fields.add(field);
        }
        return fields;
    }
    List<Map<String,Object>> inputs(List<Map<String,Object>> definition) {
        var result = new ArrayList<Map<String,Object>>();
        var members = List.of("numberValue", "timeSeconds", "textValue", "checked", "memoValue");
        var values = List.of(0, 0L, "BFS", false, " 첫 줄\n둘째 줄 ");
        for (int i=0;i<5;i++) result.add(Map.of("fieldId", definition.get(i).get("fieldId"), "type", definition.get(i).get("type"), members.get(i), values.get(i)));
        return result;
    }
    Map<String,Object> create(UUID task, List<Map<String,Object>> fields) {
        return new LinkedHashMap<>(Map.of("commandId", UUID.randomUUID(), "taskId", task, "activityType", "STUDY",
            "completionStatus", "COMPLETED", "performedAt", "2026-09-07T10:00:00+09:00", "expectedTemplateVersion", 1,
            "detail", Map.of("study", Map.of("fields", fields))));
    }
    String setup(UUID owner, UUID task, List<Map<String,Object>> fields) throws Exception {
        var template = ok(send("POST", "/templates", Map.of("commandId", UUID.randomUUID(), "name", "알고리즘",
            "domain", "STUDY", "kind", "STUDY_CATEGORY", "fields", fields), owner), 201).path("templateId").asText();
        var binding = bindings.approve(new TemplateSelectionRequest(UUID.randomUUID(), owner,
            TemplateSelectionRequest.TargetTypeEnum.TASK, task, TemplateSelectionRequest.TaskTypeEnum.STUDY, UUID.fromString(template), 1L));
        var payload = new LinkedHashMap<String,Object>(Map.of("taskId", task, "taskType", "STUDY", "status", "PLANNED",
            "scheduledDate", "2026-09-07", "templateLink", Map.of("bindingId", binding.getBindingId(), "templateId", template, "selectedTemplateVersion", 1)));
        payload.put("seriesId", null);
        consumer.receive(mapper.writeValueAsString(Map.of("eventId", UUID.randomUUID(), "type", "TASK_SCHEDULED", "version", 2,
            "aggregateVersion", 0, "occurredAt", Instant.now().toString(), "userId", owner, "payload", payload)));
        return template;
    }
    JsonNode ok(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        return mapper.readTree(response.body());
    }
    HttpResponse<String> send(String method, String path, Object body, UUID owner) throws Exception {
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder().issuer("todorok")
            .audience("todorok-api").subject(owner.toString()).issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(600))).build());
        jwt.sign(new RSASSASigner((java.security.interfaces.RSAPrivateKey) KEYS.getPrivate()));
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/activity/v1" + path))
            .header("Authorization", "Bearer " + jwt.serialize()).header("Content-Type", "application/json")
            .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body instanceof String s ? s : mapper.writeValueAsString(body)))
            .build(), HttpResponse.BodyHandlers.ofString());
    }
    static KeyPair keys() {
        try { var generator = java.security.KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair(); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
