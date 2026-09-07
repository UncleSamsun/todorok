package io.todorok.planner.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.create-schemas=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration",
        "spring.jpa.properties.hibernate.generate_statistics=true",
    }
)
class TaskHttpIntegrationTest {

    static final java.security.KeyPair KEYS = keys();

    static java.security.KeyPair keys() {
        try {
            var g = java.security.KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:17.11-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.hikari.connection-init-sql", () -> "set search_path to planner");
        r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
        r.add("todorok.auth.public-key", () ->
            Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded())
        );
        r.add("todorok.auth.private-key", () ->
            Base64.getEncoder().encodeToString(KEYS.getPrivate().getEncoded())
        );
        r.add("todorok.auth.allowed-origins", () -> "https://todorok.test");
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JsonMapper json;

    @Autowired
    jakarta.persistence.EntityManagerFactory entityManagerFactory;

    String token(UUID owner) throws Exception {
        var jwt = new SignedJWT(
            new JWSHeader(JWSAlgorithm.RS256),
            new JWTClaimsSet.Builder()
                .subject(owner.toString())
                .issuer("todorok")
                .audience("todorok-api")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .build()
        );
        jwt.sign(new RSASSASigner(KEYS.getPrivate()));
        return jwt.serialize();
    }

    HttpResponse<String> call(UUID owner, String method, String path, String body)
        throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/planner/v1" + path)
            )
                .header("Authorization", "Bearer " + token(owner))
                .header("Content-Type", "application/json")
                .method(
                    method,
                    body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body)
                )
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    JsonNode body(HttpResponse<String> r) {
        return json.readTree(r.body());
    }

    @Test
    void taskLifecycleEnforcesOwnerVersionAndAtomicOutbox() throws Exception {
        UUID owner = UUID.randomUUID(),
            other = UUID.randomUUID();
        var create = call(
            owner,
            "POST",
            "/tasks",
            "{\"title\":\"책 읽기\",\"taskType\":\"GENERAL\",\"scheduledDate\":\"2026-12-31\"}"
        );
        assertThat(create.statusCode()).as(create.body()).isEqualTo(201);
        String id = body(create).get("taskId").asText();
        assertThat(body(create).get("userId").asText()).isEqualTo(owner.toString());
        assertThat(call(other, "GET", "/tasks/" + id, null).statusCode()).isEqualTo(404);
        assertThat(call(owner, "POST", "/tasks/" + id + "/reopen", "{\"version\":0}").statusCode()).isEqualTo(409);
        assertThat(call(other, "POST", "/tasks/" + id + "/complete", "{\"version\":0}").statusCode()).isEqualTo(404);
        assertThat(call(other, "DELETE", "/tasks/" + id + "?version=0", null).statusCode()).isEqualTo(404);
        assertThat(call(other, "PATCH", "/tasks/" + id, "{\"title\":\"침범\",\"scheduledDate\":\"2026-12-31\",\"version\":0}").statusCode()).isEqualTo(404);
        assertThat(
            call(owner, "POST", "/tasks/" + id + "/complete", "{\"version\":0}").statusCode()
        ).isEqualTo(200);
        assertThat(call(owner, "POST", "/tasks/" + id + "/complete", "{\"version\":1}").statusCode()).isEqualTo(409);
        assertThat(
            call(
                owner,
                "PATCH",
                "/tasks/" + id,
                "{\"title\":\"수정\",\"scheduledDate\":\"2027-01-01\",\"version\":0}"
            ).statusCode()
        ).isEqualTo(409);
        assertThat(
            call(owner, "POST", "/tasks/" + id + "/reopen", "{\"version\":1}").statusCode()
        ).isEqualTo(200);
        assertThat(
            call(
                owner,
                "PATCH",
                "/tasks/" + id,
                "{\"title\":\"수정\",\"scheduledDate\":\"2027-01-01\",\"version\":2}"
            ).statusCode()
        ).isEqualTo(200);
        assertThat(
            call(owner, "DELETE", "/tasks/" + id + "?version=3", null).statusCode()
        ).isEqualTo(204);
        assertThat(call(owner, "GET", "/tasks/" + id, null).statusCode()).isEqualTo(404);
        assertThat(body(call(owner, "GET", "/calendar/2027-01-01", null)).get("tasks").size()).isZero();
        assertThat(body(call(owner, "GET", "/calendar?from=2027-01-01&to=2027-01-01", null)).get("days").get(0).get("totalCount").asInt()).isZero();
        assertThat(
            jdbc.queryForObject(
                "select count(*) from planner.outbox_event where aggregateid=?",
                Integer.class,
                id
            )
        ).isEqualTo(5);
        assertThat(
            jdbc.queryForList(
                "select (payload->>'aggregateVersion')::bigint from planner.outbox_event where aggregateid=? order by (payload->>'aggregateVersion')::bigint",
                Long.class,
                id
            )
        ).containsExactly(0L, 1L, 2L, 3L, 4L);
    }

    @Test
    void validationAndRecordTypesCannotBypassActivityCompletion() throws Exception {
        UUID owner = UUID.randomUUID();
        for (String type : List.of("WORKOUT", "STUDY", "CLIMBING")) {
            var c = call(
                owner,
                "POST",
                "/tasks",
                "{\"title\":\"기록\",\"taskType\":\"" +
                    type +
                    "\",\"scheduledDate\":\"2026-09-07\"}"
            );
            assertThat(c.statusCode()).as(c.body()).isEqualTo(201);
            assertThat(
                call(
                    owner,
                    "POST",
                    "/tasks/" + body(c).get("taskId").asText() + "/complete",
                    "{\"version\":0}"
                ).statusCode()
            ).isEqualTo(409);
        }
        for (String title : List.of("", "   ", "x".repeat(121)))
            assertThat(
                call(
                    owner,
                    "POST",
                    "/tasks",
                    "{\"title\":\"" +
                        title +
                        "\",\"taskType\":\"GENERAL\",\"scheduledDate\":\"2026-09-07\"}"
                ).statusCode()
            ).isEqualTo(400);
        assertThat(
            call(owner, "GET", "/calendar?from=2026-09-01&to=2026-10-13", null).statusCode()
        ).isEqualTo(400);
        assertThat(call(owner, "GET", "/calendar/2026-02-30", null).statusCode()).isEqualTo(400);
        assertThat(call(owner,"POST","/tasks","{\"title\":\"날짜 오류\",\"taskType\":\"GENERAL\",\"scheduledDate\":\"2026-02-30\"}").statusCode()).isEqualTo(400);
        assertThat(call(owner,"GET","/calendar?from=2026-09-08&to=2026-09-07",null).statusCode()).isEqualTo(400);
        var days = body(call(owner, "GET", "/calendar?from=2026-09-07&to=2026-09-08", null)).get(
            "days"
        );
        assertThat(days.size()).isEqualTo(2);
        assertThat(days.get(0).get("totalCount").asInt()).isEqualTo(3);
        assertThat(days.get(1).get("totalCount").asInt()).isZero();
        assertThat(days.get(1).get("categoryProgress").size()).isEqualTo(4);
    }

    @Test
    void noOpEditDoesNotEmitAnotherEventWithTheSameVersion() throws Exception {
        UUID owner = UUID.randomUUID();
        var created = body(
            call(
                owner,
                "POST",
                "/tasks",
                "{\"title\":\"같은 제목\",\"taskType\":\"GENERAL\",\"scheduledDate\":\"2026-09-07\"}"
            )
        );
        String id = created.get("taskId").asText();
        assertThat(
            call(
                owner,
                "PATCH",
                "/tasks/" + id,
                "{\"title\":\"같은 제목\",\"scheduledDate\":\"2026-09-07\",\"version\":0}"
            ).statusCode()
        ).isEqualTo(200);
        assertThat(
            jdbc.queryForObject(
                "select count(*) from planner.outbox_event where aggregateid=?",
                Integer.class,
                id
            )
        ).isOne();
    }

    @Test
    void failedOutboxInsertRollsBackTaskInsertAndMutation() throws Exception {
        UUID owner = UUID.randomUUID();
        var created = body(
            call(
                owner,
                "POST",
                "/tasks",
                "{\"title\":\"원본\",\"taskType\":\"GENERAL\",\"scheduledDate\":\"2026-09-07\"}"
            )
        );
        String id = created.get("taskId").asText();
        jdbc.execute(
            "alter table planner.outbox_event add constraint reject_test_owner check ((payload->>'userId') <> '" +
                owner +
                "') not valid"
        );
        try {
            assertThat(
                call(
                    owner,
                    "POST",
                    "/tasks",
                    "{\"title\":\"롤백\",\"taskType\":\"GENERAL\",\"scheduledDate\":\"2026-09-07\"}"
                ).statusCode()
            ).isEqualTo(500);
            assertThat(
                call(owner, "POST", "/tasks/" + id + "/complete", "{\"version\":0}").statusCode()
            ).isEqualTo(500);
            assertThat(
                jdbc.queryForObject(
                    "select count(*) from planner.task where user_id=?",
                    Integer.class,
                    owner
                )
            ).isOne();
            var original = body(call(owner, "GET", "/tasks/" + id, null));
            assertThat(original.get("status").asText()).isEqualTo("PLANNED");
            assertThat(original.get("version").asLong()).isZero();
            assertThat(
                jdbc.queryForObject(
                    "select count(*) from planner.outbox_event where aggregateid=?",
                    Integer.class,
                    id
                )
            ).isOne();
        } finally {
            jdbc.execute("alter table planner.outbox_event drop constraint reject_test_owner");
        }
    }

    @Test
    void projectionsUseOneQueryFor100And1000TasksAndKeepExactCategoryCounts() throws Exception {
        var stats = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        for (int size : List.of(100, 1000)) {
            UUID owner = UUID.randomUUID();
            jdbc.update(
                "insert into planner.task(id,user_id,title,task_type,scheduled_date,status,version) select gen_random_uuid(),?,'일정',case when i%2=0 then 'GENERAL' else 'STUDY' end,date '2026-09-07',case when i%4=0 then 'COMPLETED' else 'PLANNED' end,0 from generate_series(1,?) i",
                owner,
                size
            );
            stats.clear();
            var day = body(call(owner, "GET", "/calendar/2026-09-07", null));
            assertThat(day.get("tasks").size()).isEqualTo(size);
            assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
            stats.clear();
            var summary = body(call(owner, "GET", "/calendar?from=2026-09-01&to=2026-10-12", null));
            assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
            assertThat(summary.get("days").size()).isEqualTo(42);
            var target = summary.get("days").get(6);
            assertThat(target.get("totalCount").asInt()).isEqualTo(size);
            assertThat(target.get("completedCount").asInt()).isEqualTo(size / 4);
            assertThat(target.get("categoryProgress").get(0).get("totalCount").asInt()).isEqualTo(
                size / 2
            );
            assertThat(
                target.get("categoryProgress").get(0).get("completedCount").asInt()
            ).isEqualTo(size / 4);
            assertThat(target.get("categoryProgress").get(1).get("taskType").asText()).isEqualTo(
                "WORKOUT"
            );
            assertThat(target.get("categoryProgress").get(2).get("taskType").asText()).isEqualTo(
                "STUDY"
            );
            assertThat(target.get("categoryProgress").get(3).get("taskType").asText()).isEqualTo(
                "CLIMBING"
            );
            assertThat(
                body(call(UUID.randomUUID(), "GET", "/calendar/2026-09-07", null))
                    .get("tasks")
                    .size()
            ).isZero();
        }
    }
}
