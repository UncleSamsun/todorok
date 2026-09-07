package io.todorok.activity.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.create-schemas=false",
    "spring.flyway.locations=classpath:db/migration",
    "spring.jpa.hibernate.ddl-auto=none",
    "todorok.messaging.enabled=false"
})
class TemplateFoundationHttpTest {
    private static final KeyPair KEYS = keys();
    private static final UUID OWNER = UUID.randomUUID();

    @Container
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:17.11-alpine")
            .withDatabaseName("todorok")
            .withUsername("postgres")
            .withPassword("admin-password")
            .withEnv("PLANNER_DB_PASSWORD", "planner-test-password")
            .withEnv("ACTIVITY_DB_PASSWORD", "activity-test-password")
            .withEnv("NOTIFICATION_DB_PASSWORD", "notification-test-password")
            .withEnv("DEBEZIUM_DB_PASSWORD", "debezium-test-password")
            .withCopyFileToContainer(
                MountableFile.forHostPath(roleScript()),
                "/docker-entrypoint-initdb.d/001-create-service-roles.sh"
            );

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl()
            + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=activity");
        registry.add("spring.datasource.username", () -> "activity_app");
        registry.add("spring.datasource.password", () -> "activity-test-password");
        registry.add("todorok.auth.public-key", () -> Base64.getEncoder()
            .encodeToString(KEYS.getPublic().getEncoded()));
    }

    private static Path roleScript() {
        return Path.of(System.getProperty("todorok.repository.root"))
            .resolve("infra/docker/postgres/init/001-create-service-roles.sh");
    }

    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void createsAndReadsACompleteFiveTypeDefinitionWithoutCreatingTasksOrEvents() throws Exception {
        var fields = List.of(
            field("NUMBER", "문제 수", "문제"),
            field("TIME", "소요 시간", null),
            field("SHORT_TEXT", "한 줄", null),
            field("CHECK", "복습", null),
            field("MEMO", "메모", null)
        );
        var request = new LinkedHashMap<String, Object>();
        request.put("commandId", UUID.randomUUID());
        request.put("name", "알고리즘");
        request.put("domain", "STUDY");
        request.put("kind", "STUDY_CATEGORY");
        request.put("fields", fields);

        var created = send("POST", "/templates", mapper.writeValueAsString(request), OWNER);

        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode body = mapper.readTree(created.body());
        assertThat(body.get("revision").asLong()).isZero();
        assertThat(body.get("currentVersion").get("templateVersion").asLong()).isEqualTo(1);
        assertThat(body.get("currentVersion").get("fields").size()).isEqualTo(5);
        assertThat(body.get("currentVersion").get("fields").get(1).get("unit").asText()).isEqualTo("초");
        assertThat(jdbc.queryForObject("select count(*) from task_reference", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from outbox_event", Integer.class)).isZero();

        var read = send("GET", "/templates/" + body.get("templateId").asText(), null, OWNER);
        assertThat(read.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(read.body())).isEqualTo(body);
    }

    @Test
    void versionsAreAppendOnlyCommandsReplayAndArchiveOnlyChangesIdentityState() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID command = UUID.randomUUID();
        UUID retained = UUID.randomUUID();
        UUID removed = UUID.randomUUID();
        String createBody = templateBody(command, "처음", "WORKOUT", "FREE_WORKOUT", List.of(
            field(retained, "횟수", "NUMBER", "회"),
            field(removed, "설명", "SHORT_TEXT", null)
        ));
        var created = send("POST", "/templates", createBody, owner);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(send("POST", "/templates", createBody, owner).body()).isEqualTo(created.body());
        String templateId = mapper.readTree(created.body()).get("templateId").asText();
        assertThat(jdbc.queryForObject(
            "select count(*) from record_template where user_id=?", Integer.class, owner)).isEqualTo(1);

        UUID versionCommand = UUID.randomUUID();
        String version2Body = versionBody(versionCommand, 0, "두 번째", List.of(
            field(retained, "반복", "NUMBER", "회")
        ));
        var version2 = send("POST", "/templates/" + templateId + "/versions", version2Body, owner);
        assertThat(version2.statusCode()).isEqualTo(201);
        assertThat(send("POST", "/templates/" + templateId + "/versions", version2Body, owner).body())
            .isEqualTo(version2.body());
        JsonNode version2Json = mapper.readTree(version2.body());
        assertThat(version2Json.get("revision").asLong()).isEqualTo(1);
        assertThat(version2Json.get("currentVersion").get("templateVersion").asLong()).isEqualTo(2);

        var badReuse = send("POST", "/templates/" + templateId + "/versions",
            versionBody(UUID.randomUUID(), 1, "재사용", List.of(field(removed, "설명", "SHORT_TEXT", null))), owner);
        assertProblem(badReuse, 400, "FIELD_UNKNOWN");
        var badType = send("POST", "/templates/" + templateId + "/versions",
            versionBody(UUID.randomUUID(), 1, "형식 변경", List.of(field(retained, "완료", "CHECK", null))), owner);
        assertProblem(badType, 400, "FIELD_TYPE_IMMUTABLE");
        UUID foreignField = UUID.randomUUID();
        send("POST", "/templates", templateBody(UUID.randomUUID(), "다른 템플릿", "STUDY", "STUDY_CATEGORY",
            List.of(field(foreignField, "외부", "NUMBER", null))), UUID.randomUUID());
        assertProblem(send("POST", "/templates/" + templateId + "/versions",
            versionBody(UUID.randomUUID(), 1, "타 템플릿 ID", List.of(field(foreignField, "외부", "NUMBER", null))), owner),
            400, "FIELD_UNKNOWN");
        assertThat(jdbc.queryForObject(
            "select count(*) from template_version where template_id=?", Integer.class, UUID.fromString(templateId)))
            .isEqualTo(2);

        var historical = send("GET", "/templates/" + templateId + "/versions/1", null, owner);
        assertThat(historical.statusCode()).isEqualTo(200);
        JsonNode historicalJson = mapper.readTree(historical.body());
        assertThat(historicalJson.get("name").asText()).isEqualTo("처음");
        assertThat(historicalJson.get("fields").get(0).get("name").asText()).isEqualTo("횟수");
        assertThat(historicalJson.get("fields").get(1).get("name").asText()).isEqualTo("설명");

        UUID archiveCommand = UUID.randomUUID();
        String archiveBody = archiveBody(archiveCommand, 1);
        var archived = send("POST", "/templates/" + templateId + "/archive", archiveBody, owner);
        assertThat(archived.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(archived.body()).get("archived").asBoolean()).isTrue();
        assertThat(mapper.readTree(archived.body()).get("revision").asLong()).isEqualTo(2);
        assertThat(send("POST", "/templates/" + templateId + "/archive", archiveBody, owner).body())
            .isEqualTo(archived.body());
        assertProblem(send("POST", "/templates/" + templateId + "/versions",
            versionBody(UUID.randomUUID(), 2, "금지", List.of()), owner), 409, "TEMPLATE_ARCHIVED");
        assertThat(send("GET", "/templates/" + templateId + "/versions/1", null, owner).statusCode())
            .isEqualTo(200);
        assertThat(mapper.readTree(send("GET", "/templates", null, owner).body()).get("items").size()).isZero();
        assertThat(mapper.readTree(send("GET", "/templates?includeArchived=true", null, owner).body())
            .get("items").size()).isEqualTo(1);
    }

    @Test
    void enforcesOwnerIsolationCommandReuseValidationAndRollback() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID command = UUID.randomUUID();
        String valid = templateBody(command, "내 템플릿", "CLIMBING", "FREE_HANGBOARD", List.of());
        var created = send("POST", "/templates", valid, owner);
        assertThat(created.statusCode()).isEqualTo(201);
        String id = mapper.readTree(created.body()).get("templateId").asText();
        assertProblem(send("GET", "/templates/" + id, null, other), 404, "NOT_FOUND");
        assertProblem(send("GET", "/templates/" + id + "/versions/1", null, other), 404, "NOT_FOUND");
        assertThat(mapper.readTree(send("GET", "/templates?includeArchived=true", null, other).body())
            .get("items").size()).isZero();

        assertProblem(send("POST", "/templates",
            templateBody(command, "다른 payload", "CLIMBING", "FREE_HANGBOARD", List.of()), owner),
            409, "COMMAND_REUSE");
        assertProblem(send("POST", "/templates",
            templateBody(UUID.randomUUID(), "   ", "STUDY", "STUDY_CATEGORY", List.of()), owner),
            400, "VALIDATION_FAILED");
        assertProblem(send("POST", "/templates",
            templateBody(UUID.randomUUID(), "잘못된 조합", "STUDY", "FREE_WORKOUT", List.of()), owner),
            400, "VALIDATION_FAILED");
        UUID duplicate = UUID.randomUUID();
        assertProblem(send("POST", "/templates",
            templateBody(UUID.randomUUID(), "중복", "STUDY", "STUDY_CATEGORY", List.of(
                field(duplicate, "하나", "NUMBER", null),
                field(duplicate, "둘", "NUMBER", null)
            )), owner), 400, "FIELD_ID_DUPLICATE");
        assertProblem(send("POST", "/templates",
            templateBody(UUID.randomUUID(), "단위", "STUDY", "STUDY_CATEGORY", List.of(
                field(UUID.randomUUID(), "체크", "CHECK", "회")
            )), owner), 400, "VALIDATION_FAILED");
        assertThat(jdbc.queryForObject(
            "select count(*) from record_template where user_id=?", Integer.class, owner)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "select count(*) from template_management_command where user_id=?", Integer.class, owner)).isEqualTo(1);
    }

    @Test
    void pagesByOwnerAndFiltersAndRejectsCursorFilterMismatch() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        send("POST", "/templates", templateBody(UUID.randomUUID(), "공부", "STUDY", "STUDY_CATEGORY", List.of()), owner);
        send("POST", "/templates", templateBody(UUID.randomUUID(), "운동", "WORKOUT", "FREE_WORKOUT", List.of()), owner);
        send("POST", "/templates", templateBody(UUID.randomUUID(), "클라이밍", "CLIMBING", "CLIMBING_SESSION", List.of()), owner);
        send("POST", "/templates", templateBody(UUID.randomUUID(), "다른 사람", "STUDY", "STUDY_CATEGORY", List.of()), other);

        JsonNode first = mapper.readTree(send("GET", "/templates?limit=2", null, owner).body());
        assertThat(first.get("items").size()).isEqualTo(2);
        String cursor = java.net.URLEncoder.encode(first.get("nextCursor").asText(), java.nio.charset.StandardCharsets.UTF_8);
        JsonNode second = mapper.readTree(send("GET", "/templates?limit=2&cursor=" + cursor, null, owner).body());
        assertThat(second.get("items").size()).isEqualTo(1);
        assertThat(second.has("nextCursor")).isFalse();
        var ids = new java.util.HashSet<String>();
        first.get("items").forEach(item -> ids.add(item.get("templateId").asText()));
        second.get("items").forEach(item -> ids.add(item.get("templateId").asText()));
        assertThat(ids).hasSize(3);
        assertThat(mapper.readTree(send("GET", "/templates?domain=STUDY", null, owner).body())
            .get("items").size()).isEqualTo(1);
        assertProblem(send("GET", "/templates?limit=2&domain=STUDY&cursor=" + cursor, null, owner),
            400, "INVALID_CURSOR");
        assertProblem(send("GET", "/templates?cursor=not-base64", null, owner), 400, "INVALID_CURSOR");
    }

    @Test
    void storesHundredsOfFieldsWithoutAProductCountLimitAndPreservesOrder() throws Exception {
        UUID owner = UUID.randomUUID();
        var fields = new java.util.ArrayList<Map<String, Object>>();
        for (int index = 0; index < 512; index++) {
            fields.add(field(UUID.randomUUID(), "항목 " + index, "NUMBER", index % 2 == 0 ? "회" : null));
        }
        var response = send("POST", "/templates",
            templateBody(UUID.randomUUID(), "많은 항목", "STUDY", "STUDY_CATEGORY", fields), owner);
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode stored = mapper.readTree(response.body()).get("currentVersion").get("fields");
        assertThat(stored.size()).isEqualTo(512);
        assertThat(stored.get(0).get("name").asText()).isEqualTo("항목 0");
        assertThat(stored.get(0).get("position").asInt()).isZero();
        assertThat(stored.get(511).get("name").asText()).isEqualTo("항목 511");
        assertThat(stored.get(511).get("position").asInt()).isEqualTo(511);
    }

    @Test
    void serializesConcurrentVersionAndArchiveChangesAtOneRevision() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID duplicateCommand = UUID.randomUUID();
        String duplicateBody = templateBody(
            duplicateCommand, "동일 command", "STUDY", "STUDY_CATEGORY", List.of());
        var duplicateResults = race(
            () -> send("POST", "/templates", duplicateBody, owner),
            () -> send("POST", "/templates", duplicateBody, owner)
        );
        assertThat(duplicateResults.stream().map(HttpResponse::statusCode)).containsOnly(201);
        assertThat(duplicateResults.get(0).body()).isEqualTo(duplicateResults.get(1).body());
        assertThat(jdbc.queryForObject(
            "select count(*) from record_template where user_id=?", Integer.class, owner)).isEqualTo(1);

        JsonNode versionFixture = mapper.readTree(send("POST", "/templates",
            templateBody(UUID.randomUUID(), "경쟁 버전", "STUDY", "STUDY_CATEGORY", List.of()), owner).body());
        String versionId = versionFixture.get("templateId").asText();
        var versionResults = race(
            () -> send("POST", "/templates/" + versionId + "/versions",
                versionBody(UUID.randomUUID(), 0, "A", List.of()), owner),
            () -> send("POST", "/templates/" + versionId + "/versions",
                versionBody(UUID.randomUUID(), 0, "B", List.of()), owner)
        );
        assertThat(versionResults.stream().map(HttpResponse::statusCode)).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("select count(*) from template_version where template_id=?",
            Integer.class, UUID.fromString(versionId))).isEqualTo(2);

        JsonNode archiveFixture = mapper.readTree(send("POST", "/templates",
            templateBody(UUID.randomUUID(), "경쟁 보관", "STUDY", "STUDY_CATEGORY", List.of()), owner).body());
        String archiveId = archiveFixture.get("templateId").asText();
        var archiveResults = race(
            () -> send("POST", "/templates/" + archiveId + "/versions",
                versionBody(UUID.randomUUID(), 0, "새 버전", List.of()), owner),
            () -> send("POST", "/templates/" + archiveId + "/archive",
                archiveBody(UUID.randomUUID(), 0), owner)
        );
        assertThat(archiveResults.stream().map(HttpResponse::statusCode).filter(code -> code == 409).count()).isEqualTo(1);
        assertThat(archiveResults.stream().map(HttpResponse::statusCode).filter(code -> code == 200 || code == 201).count())
            .isEqualTo(1);
        JsonNode finalState = mapper.readTree(send("GET", "/templates/" + archiveId, null, owner).body());
        assertThat(finalState.get("revision").asLong()).isEqualTo(1);
        assertThat(finalState.get("archived").asBoolean()
            || finalState.get("currentVersion").get("templateVersion").asLong() == 2).isTrue();
    }

    @Test
    void rejectsUnknownDuplicateOversizedAndCompressedBodiesOnlyOnTemplateWrites() throws Exception {
        UUID owner = UUID.randomUUID();
        int before = jdbc.queryForObject("select count(*) from record_template", Integer.class);
        String command = UUID.randomUUID().toString();
        String unknown = "{\"commandId\":\"" + command + "\",\"name\":\"x\",\"domain\":\"STUDY\","
            + "\"kind\":\"STUDY_CATEGORY\",\"fields\":[],\"unknown\":true}";
        assertThat(send("POST", "/templates", unknown, owner).statusCode()).isEqualTo(400);
        String duplicate = "{\"commandId\":\"" + UUID.randomUUID() + "\",\"name\":\"x\",\"name\":\"y\","
            + "\"domain\":\"STUDY\",\"kind\":\"STUDY_CATEGORY\",\"fields\":[]}";
        assertProblem(send("POST", "/templates", duplicate, owner), 400, "MALFORMED_JSON");

        String base = templateBody(UUID.randomUUID(), "경계", "STUDY", "STUDY_CATEGORY", List.of());
        String exact = base + " ".repeat(TemplateRequestBodyAdvice.MAX_BODY_BYTES - base.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        assertThat(exact.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(TemplateRequestBodyAdvice.MAX_BODY_BYTES);
        assertThat(send("POST", "/templates", exact, owner).statusCode()).isEqualTo(201);
        byte[] over = (base + " ".repeat(TemplateRequestBodyAdvice.MAX_BODY_BYTES + 1
            - base.getBytes(java.nio.charset.StandardCharsets.UTF_8).length))
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var chunked = request("POST", "/templates", HttpRequest.BodyPublishers.ofInputStream(
            () -> new java.io.ByteArrayInputStream(over)), owner, Map.of("Content-Type", "application/json"));
        assertProblem(chunked, 413, "PAYLOAD_TOO_LARGE");
        var encoded = request("POST", "/templates", HttpRequest.BodyPublishers.ofString(base), owner,
            Map.of("Content-Type", "application/json", "Content-Encoding", "gzip"));
        assertProblem(encoded, 415, "UNSUPPORTED_CONTENT_ENCODING");
        assertThat(jdbc.queryForObject("select count(*) from record_template", Integer.class)).isEqualTo(before + 1);

        String activityDuplicate = "{\"commandId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + UUID.randomUUID()
            + "\",\"activityType\":\"STUDY\",\"activityType\":\"STUDY\",\"completionStatus\":\"PARTIAL\","
            + "\"performedAt\":\"2026-09-07T10:00:00+09:00\",\"detail\":{}}";
        assertProblem(send("POST", "/activities", activityDuplicate, owner), 409, "TASK_REFERENCE_PENDING");
    }

    @Test
    void databaseRejectsMutationOrDeletionOfEveryHistoricalDefinitionTable() throws Exception {
        UUID owner = UUID.randomUUID();
        JsonNode created = mapper.readTree(send("POST", "/templates",
            templateBody(UUID.randomUUID(), "불변", "STUDY", "STUDY_CATEGORY", List.of(
                field(UUID.randomUUID(), "숫자", "NUMBER", null)
            )), owner).body());
        UUID templateId = UUID.fromString(created.get("templateId").asText());
        for (String statement : List.of(
            "update record_template set user_id='" + UUID.randomUUID() + "' where id='" + templateId + "'",
            "update record_template set revision=revision+2 where id='" + templateId + "'",
            "delete from record_template where id='" + templateId + "'",
            "update template_version set name='변조' where template_id='" + templateId + "'",
            "delete from template_version where template_id='" + templateId + "'",
            "update template_field_identity set type='CHECK' where template_id='" + templateId + "'",
            "delete from template_field_identity where template_id='" + templateId + "'",
            "update template_field_definition set name='변조' where template_id='" + templateId + "'",
            "delete from template_field_definition where template_id='" + templateId + "'",
            "update template_management_command set status_code=200 where user_id='" + owner + "'",
            "delete from template_management_command where user_id='" + owner + "'"
        )) {
            assertThatThrownBy(() -> jdbc.update(statement)).as(statement).isInstanceOf(DataAccessException.class);
        }
        UUID invalidTimeField = UUID.randomUUID();
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbc.update("insert into template_field_identity(template_id,field_id,created_version,type) values (?,?,1,'TIME')",
                templateId, invalidTimeField);
            jdbc.update("insert into template_field_definition(template_id,version,field_id,name,type,unit,position) values (?,1,?,'시간','TIME',null,1)",
                templateId, invalidTimeField);
        })).isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("select count(*) from template_field_identity where field_id=?",
            Integer.class, invalidTimeField)).isZero();
        assertThat(mapper.readTree(send("GET", "/templates/" + templateId, null, owner).body())
            .get("currentVersion").get("name").asText()).isEqualTo("불변");
    }

    private Map<String, Object> field(String type, String name, String unit) {
        return field(UUID.randomUUID(), name, type, unit);
    }

    private Map<String, Object> field(UUID id, String name, String type, String unit) {
        var field = new LinkedHashMap<String, Object>();
        field.put("fieldId", id);
        field.put("name", name);
        field.put("type", type);
        if (unit != null) field.put("unit", unit);
        return field;
    }

    private String templateBody(
        UUID commandId,
        String name,
        String domain,
        String kind,
        List<Map<String, Object>> fields
    ) {
        var request = new LinkedHashMap<String, Object>();
        request.put("commandId", commandId);
        request.put("name", name);
        request.put("domain", domain);
        request.put("kind", kind);
        request.put("fields", fields);
        return mapper.writeValueAsString(request);
    }

    private String versionBody(
        UUID commandId,
        long expectedRevision,
        String name,
        List<Map<String, Object>> fields
    ) {
        var request = new LinkedHashMap<String, Object>();
        request.put("commandId", commandId);
        request.put("expectedRevision", expectedRevision);
        request.put("name", name);
        request.put("fields", fields);
        return mapper.writeValueAsString(request);
    }

    private String archiveBody(UUID commandId, long expectedRevision) {
        return mapper.writeValueAsString(Map.of(
            "commandId", commandId,
            "expectedRevision", expectedRevision
        ));
    }

    private List<HttpResponse<String>> race(
        java.util.concurrent.Callable<HttpResponse<String>> left,
        java.util.concurrent.Callable<HttpResponse<String>> right
    ) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var wrappedLeft = pool.submit(() -> {
                ready.countDown();
                start.await();
                return left.call();
            });
            var wrappedRight = pool.submit(() -> {
                ready.countDown();
                start.await();
                return right.call();
            });
            assertThat(ready.await(15, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(wrappedLeft.get(), wrappedRight.get());
        } finally {
            start.countDown();
        }
    }

    private void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
            .startsWith("application/problem+json");
        assertThat(mapper.readTree(response.body()).get("code").asText()).isEqualTo(code);
    }

    private HttpResponse<String> send(String method, String path, String body, UUID owner) throws Exception {
        return request(
            method,
            path,
            body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body),
            owner,
            body == null ? Map.of() : Map.of("Content-Type", "application/json")
        );
    }

    private HttpResponse<String> request(
        String method,
        String path,
        HttpRequest.BodyPublisher body,
        UUID owner,
        Map<String, String> headers
    ) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/activity/v1" + path))
            .header("Authorization", "Bearer " + token(owner));
        headers.forEach(request::header);
        return HttpClient.newHttpClient().send(
            request.method(method, body).build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private static String token(UUID owner) throws Exception {
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
            .issuer("todorok").audience("todorok-api").subject(owner.toString()).issueTime(new Date())
            .expirationTime(Date.from(Instant.now().plusSeconds(300))).build());
        jwt.sign(new RSASSASigner((java.security.interfaces.RSAPrivateKey) KEYS.getPrivate()));
        return jwt.serialize();
    }

    private static KeyPair keys() {
        try {
            var generator = java.security.KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
