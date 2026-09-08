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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("duplicateSelectionCases")
    void duplicateSelectionCannotHideCoercedVersion(String resource, String invalid) throws Exception {
        UUID owner=UUID.randomUUID();
        String dates=resource.equals("tasks") ? "\"scheduledDate\":\"2026-09-09\"" :
            "\"startDate\":\"2026-09-09\",\"rule\":{\"frequency\":\"DAILY\",\"interval\":1,\"weekdays\":[],\"monthDay\":1}";
        String base="\"title\":\"중복 선택 검증\",\"taskType\":\"STUDY\","+dates;
        String selection="\"templateSelection\":{\"templateId\":\""+UUID.randomUUID()+"\",\"expectedTemplateVersion\":";
        String request="{\"commandId\":\""+UUID.randomUUID()+"\","+base+","+selection+invalid+"},\"templateSelection\":null}";
        var response=call(owner,"POST","/"+resource,request);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        assertThat(body(response).path("code").asText()).isEqualTo("MALFORMED_JSON");
        assertThat(jdbc.queryForObject("select count(*) from planner.planner_creation_command where owner_id=?",Integer.class,owner)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from planner.task where user_id=?",Integer.class,owner)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from planner.task_series where user_id=?",Integer.class,owner)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from planner.outbox_event where payload->>'userId'=?",Integer.class,owner.toString())).isZero();
        var valid=call(owner,"POST","/"+resource,"{\"commandId\":\""+UUID.randomUUID()+"\","+base+","+selection+"1}}");
        assertThat(valid.statusCode()).as(valid.body()).isEqualTo(503);
        assertThat(body(valid).path("code").asText()).isEqualTo("TEMPLATE_SERVICE_UNAVAILABLE");
        assertThat(jdbc.queryForObject("select count(*) from planner.planner_creation_command where owner_id=?",Integer.class,owner)).isOne();
        assertThat(call(owner,"POST","/"+resource,"{"+base+"}").statusCode()).isEqualTo(201);
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> duplicateSelectionCases() {
        return java.util.stream.Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("tasks","1.5"),
            org.junit.jupiter.params.provider.Arguments.of("tasks","\"1\""),
            org.junit.jupiter.params.provider.Arguments.of("series","1.5"),
            org.junit.jupiter.params.provider.Arguments.of("series","\"1\""));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"tasks","series"})
    void templateVersionMustBeAnIntegerBeforeRegisteringAnyCommand(String resource) throws Exception {
        UUID owner=UUID.randomUUID();
        String dates=resource.equals("tasks") ? "\"scheduledDate\":\"2026-09-09\"" :
            "\"startDate\":\"2026-09-09\",\"rule\":{\"frequency\":\"DAILY\",\"interval\":1,\"weekdays\":[],\"monthDay\":1}";
        String suffix=",\"title\":\"정수 선택\",\"taskType\":\"STUDY\","+dates+",\"templateSelection\":{\"templateId\":\""+UUID.randomUUID()+"\",\"expectedTemplateVersion\":";
        for(String invalid:List.of("1.5","\"1\"")) {
            var response=call(owner,"POST","/"+resource,"{\"commandId\":\""+UUID.randomUUID()+"\""+suffix+invalid+"}}");
            assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
        }
        assertThat(jdbc.queryForObject("select count(*) from planner.planner_creation_command where owner_id=?",Integer.class,owner)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from planner.task where user_id=?",Integer.class,owner)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from planner.task_series where user_id=?",Integer.class,owner)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from planner.outbox_event where payload->>'userId'=?",Integer.class,owner.toString())).isZero();
        var valid=call(owner,"POST","/"+resource,"{\"commandId\":\""+UUID.randomUUID()+"\""+suffix+"1}}");
        assertThat(valid.statusCode()).as(valid.body()).isEqualTo(503);
        assertThat(body(valid).path("code").asText()).isEqualTo("TEMPLATE_SERVICE_UNAVAILABLE");
        assertThat(jdbc.queryForObject("select count(*) from planner.planner_creation_command where owner_id=?",Integer.class,owner)).isOne();
    }

    @Test
    void templateSelectionRequiresCommandAndUnavailableServiceLeavesNoTask() throws Exception {
        UUID owner = UUID.randomUUID();
        String selection = "\"templateSelection\":{\"templateId\":\"" + UUID.randomUUID() + "\",\"expectedTemplateVersion\":1}";
        String base = "\"title\":\"연결\",\"taskType\":\"STUDY\",\"scheduledDate\":\"2026-09-09\",";
        assertThat(call(owner, "POST", "/tasks", "{" + base + selection + "}").statusCode()).isEqualTo(400);
        String request = "{" + base + selection + ",\"commandId\":\"" + UUID.randomUUID() + "\"}";
        var result = call(owner, "POST", "/tasks", request);
        assertThat(result.statusCode()).as(result.body()).isEqualTo(503);
        assertThat(body(result).get("code").asText()).isEqualTo("TEMPLATE_SERVICE_UNAVAILABLE");
        assertThat(call(owner, "POST", "/tasks", request.replace(selection + ",", "")).statusCode()).isEqualTo(409);
        assertThat(body(call(owner, "GET", "/calendar/2026-09-09", null)).get("tasks").size()).isZero();
    }

    @Test
    void dailyNotesAreVersionedIsolatedAndValidateLimits() throws Exception {
        UUID owner = UUID.randomUUID(),
            other = UUID.randomUUID();
        String path = "/notes/2026-09-09";
        var missing = call(owner, "GET", path, null);
        assertThat(missing.statusCode()).isEqualTo(200);
        assertThat(body(missing).get("version").isNull()).isTrue();
        assertThat(body(missing).get("content").asText()).isEmpty();
        var created = call(
            owner,
            "PATCH",
            path,
            "{\"content\":\"메모\",\"expectedVersion\":null}"
        );
        assertThat(created.statusCode()).isEqualTo(200);
        assertThat(body(created).get("version").asLong()).isZero();
        assertThat(call(other, "PATCH", path, "{\"content\":\"other\",\"expectedVersion\":0}").statusCode()).isEqualTo(409);
        assertThat(
            body(call(other, "GET", path, null))
                .get("version")
                .isNull()
        ).isTrue();
        assertThat(call(other, "PATCH", path, "{\"content\":\"other\",\"expectedVersion\":null}").statusCode()).isEqualTo(200);
        assertThat(body(call(owner, "GET", path, null)).get("content").asText()).isEqualTo("메모");
        assertThat(
            call(
                owner,
                "PATCH",
                path,
                "{\"content\":\"\",\"expectedVersion\":null}"
            ).statusCode()
        ).isEqualTo(409);
        assertThat(
            call(owner, "PATCH", path, "{\"content\":\"\",\"expectedVersion\":0}").statusCode()
        ).isEqualTo(200);
        assertThat(
            body(call(owner, "GET", path, null))
                .get("version")
                .asLong()
        ).isEqualTo(1);
        assertThat(
            call(
                owner,
                "PATCH",
                path,
                "{\"content\":\"old\",\"expectedVersion\":0}"
            ).statusCode()
        ).isEqualTo(409);
        assertThat(
            call(
                owner,
                "PATCH",
                path,
                "{\"content\":\"" + "가".repeat(20000) + "\",\"expectedVersion\":1}"
            ).statusCode()
        ).isEqualTo(200);
        assertThat(
            call(
                owner,
                "PATCH",
                path,
                "{\"content\":\"" + "가".repeat(20001) + "\",\"expectedVersion\":2}"
            ).statusCode()
        ).isEqualTo(400);
        assertThat(call(owner, "GET", "/notes/2026-02-30", null).statusCode()).isEqualTo(400);
        assertThat(
            call(owner, "PATCH", path, "{\"content\":null,\"expectedVersion\":2}").statusCode()
        ).isEqualTo(400);
    }

    @Test
    void savesThemePreferencePerOwnerWithOptimisticRevision() throws Exception {
        UUID owner = UUID.randomUUID(), other = UUID.randomUUID();
        assertThat(body(call(owner, "GET", "/preferences", null)).path("theme").asText()).isEqualTo("SYSTEM");
        var saved = body(call(owner, "PUT", "/preferences", "{\"theme\":\"DARK\",\"expectedRevision\":0}"));
        assertThat(saved.path("theme").asText()).isEqualTo("DARK");
        assertThat(saved.path("revision").asLong()).isZero();
        assertThat(body(call(other, "GET", "/preferences", null)).path("theme").asText()).isEqualTo("SYSTEM");
        assertThat(call(owner, "PUT", "/preferences", "{\"theme\":\"LIGHT\",\"expectedRevision\":0}").statusCode()).isEqualTo(200);
        assertThat(call(owner, "PUT", "/preferences", "{\"theme\":\"SYSTEM\",\"expectedRevision\":0}").statusCode()).isEqualTo(409);
    }

    @Test
    void concurrentFirstNoteWriteHasOneWinner() throws Exception {
        UUID owner = UUID.randomUUID();
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var requests = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int i = 0; i < 8; i++) {
                int writer = i;
                requests.add(
                    executor.submit(() -> {
                        start.await();
                        return call(
                            owner,
                            "PATCH",
                            "/notes/2026-09-10",
                            "{\"content\":\"writer" + writer + "\",\"expectedVersion\":null}"
                        ).statusCode();
                    })
                );
            }
            start.countDown();
            var statuses = new ArrayList<Integer>();
            for (var request : requests) statuses.add(request.get());
            assertThat(statuses).containsOnly(200, 409);
            assertThat(
                statuses
                    .stream()
                    .filter(status -> status == 200)
                    .count()
            ).isOne();
        }
        assertThat(
            jdbc.queryForObject(
                "select count(*) from planner.daily_note where user_id=?",
                Integer.class,
                owner
            )
        ).isOne();
    }

    @Test
    void taskAndSeriesNotesPreserveOmissionAndAllowExplicitClear() throws Exception {
        UUID owner = UUID.randomUUID();
        String s = series(owner, "GENERAL", "2026-09-09", null, DAILY),
            id = active(s);
        String patch = "{\"title\":\"반복\",\"scheduledDate\":\"2026-09-09\",\"version\":0";
        assertThat(
            body(call(owner, "PATCH", "/tasks/" + id, patch + "}"))
                .get("note")
                .asText()
        ).isEqualTo("유지할 메모");
        assertThat(
            body(call(owner, "PATCH", "/tasks/" + id, patch + ",\"note\":\"이번 회차만\"}"))
                .get("note")
                .asText()
        ).isEqualTo("이번 회차만");
        assertThat(
            call(
                owner,
                "PATCH",
                "/tasks/" + id,
                "{\"title\":\"반복\",\"scheduledDate\":\"2026-09-09\",\"version\":1,\"note\":\"" +
                    "가".repeat(20001) +
                    "\"}"
            ).statusCode()
        ).isEqualTo(400);
        String seriesPatch = "{\"title\":\"반복\",\"rule\":" + DAILY + ",\"version\":0";
        assertThat(
            body(call(owner, "PATCH", "/series/" + s, seriesPatch + "}"))
                .get("note")
                .asText()
        ).isEqualTo("유지할 메모");
        assertThat(
            call(owner, "POST", "/tasks/" + id + "/complete", "{\"version\":1}").statusCode()
        ).isEqualTo(200);
        assertThat(
            body(call(owner, "GET", "/tasks/" + active(s), null))
                .get("note")
                .asText()
        ).isEqualTo("유지할 메모");
        var currentSeries = body(call(owner, "GET", "/series/" + s, null));
        assertThat(
            body(
                call(
                    owner,
                    "PATCH",
                    "/series/" + s,
                    "{\"title\":\"반복\",\"rule\":" +
                        DAILY +
                        ",\"version\":" +
                        currentSeries.get("version").asLong() +
                        ",\"note\":\"\"}"
                )
            )
                .get("note")
                .asText()
        ).isEmpty();
        String next = active(s);
        assertThat(
            body(call(owner, "PATCH", "/tasks/" + next, patch + ",\"note\":\"\"}"))
                .get("note")
                .asText()
        ).isEmpty();
        assertThat(
            body(call(owner, "GET", "/notes/2026-09-09", null))
                .get("version")
                .isNull()
        ).isTrue();
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class FixedTime {

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        java.time.Clock fixedClock() {
            return java.time.Clock.fixed(
                Instant.parse("2026-09-08T15:00:00Z"),
                java.time.ZoneOffset.UTC
            );
        }
    }

    @Autowired
    io.todorok.planner.series.SeriesService seriesService;

    String series(UUID owner, String type, String start, String end, String rule)
        throws Exception {
        var response = call(
            owner,
            "POST",
            "/series",
            "{\"title\":\"반복\",\"note\":\"유지할 메모\",\"taskType\":\"" +
                type +
                "\",\"startDate\":\"" +
                start +
                "\"," +
                (end == null ? "" : "\"endDate\":\"" + end + "\",") +
                "\"rule\":" +
                rule +
                "}"
        );
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return body(response).get("seriesId").asText();
    }

    static final String DAILY =
        "{\"frequency\":\"DAILY\",\"interval\":2,\"weekdays\":[],\"monthDay\":1}";
    @Test
    void historicalFiniteOccurrencesStayReservedAfterGeneralRecompletion()
        throws Exception {
        for (String terminal : List.of("complete", "skip", "delete")) {
            UUID owner = UUID.randomUUID();
            String s = series(
                owner,
                "GENERAL",
                "2026-09-07",
                "2026-09-09",
                DAILY
            );
            String first = active(s);
            assertThat(
                call(
                    owner,
                    "POST",
                    "/tasks/" + first + "/complete",
                    "{\"version\":0}"
                ).statusCode()
            ).isEqualTo(200);
            String second = active(s);
            if (terminal.equals("delete")) assertThat(
                call(
                    owner,
                    "DELETE",
                    "/tasks/" + second + "?version=0",
                    null
                ).statusCode()
            ).isEqualTo(204);
            else assertThat(
                call(
                    owner,
                    "POST",
                    "/tasks/" + second + "/" + terminal,
                    "{\"version\":0}"
                ).statusCode()
            ).isEqualTo(200);
            assertThat(
                call(
                    owner,
                    "POST",
                    "/tasks/" + first + "/reopen",
                    "{\"version\":1}"
                ).statusCode()
            ).isEqualTo(200);
            assertThat(
                call(
                    owner,
                    "POST",
                    "/tasks/" + first + "/complete",
                    "{\"version\":2}"
                ).statusCode()
            ).isEqualTo(200);
            assertThat(
                jdbc.queryForObject(
                    "select count(*) from planner.task where series_id=?::uuid",
                    Integer.class,
                    s
                )
            ).isEqualTo(2);
            assertThat(activeCount(s)).isZero();
            assertThat(
                jdbc.queryForObject(
                    "select status from planner.task where id=?::uuid",
                    String.class,
                    second
                )
            ).isEqualTo(
                switch (terminal) {
                    case "complete" -> "COMPLETED";
                    case "skip" -> "SKIPPED";
                    default -> "DELETED";
                }
            );
        }
    }

    @Test
    void ongoingSeriesContinuesAfterLatestReservedDeletedOccurrence()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String s = series(owner, "GENERAL", "2026-09-07", null, DAILY),
            first = active(s);
        assertThat(
            call(
                owner,
                "POST",
                "/tasks/" + first + "/complete",
                "{\"version\":0}"
            ).statusCode()
        ).isEqualTo(200);
        String second = active(s);
        assertThat(
            call(
                owner,
                "DELETE",
                "/tasks/" + second + "?version=0",
                null
            ).statusCode()
        ).isEqualTo(204);
        assertThat(
            call(
                owner,
                "POST",
                "/tasks/" + first + "/reopen",
                "{\"version\":1}"
            ).statusCode()
        ).isEqualTo(200);
        assertThat(
            call(
                owner,
                "POST",
                "/tasks/" + first + "/complete",
                "{\"version\":2}"
            ).statusCode()
        ).isEqualTo(200);
        assertThat(
            body(call(owner, "GET", "/tasks/" + active(s), null))
                .get("occurrenceDate")
                .asText()
        ).isEqualTo("2026-09-11");
        assertThat(
            jdbc.queryForObject(
                "select count(*) from planner.task where series_id=?::uuid",
                Integer.class,
                s
            )
        ).isEqualTo(3);
        assertThat(
            jdbc.queryForObject(
                "select status from planner.task where id=?::uuid",
                String.class,
                second
            )
        ).isEqualTo("DELETED");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            jdbc.update(
                "insert into planner.task(id,user_id,title,task_type,scheduled_date,status,version,series_id,occurrence_date) select gen_random_uuid(),user_id,title,task_type,scheduled_date,'DELETED',0,series_id,occurrence_date from planner.task where id=?::uuid",
                second
            )
        )
            .isInstanceOf(
                org.springframework.dao.DataIntegrityViolationException.class
            )
            .hasMessageContaining("task_series_occurrence_unique");
    }

    static final String MONDAY =
        "{\"frequency\":\"WEEKLY\",\"interval\":1,\"weekdays\":[1],\"monthDay\":1}";

    String active(String id) {
        return jdbc.queryForObject(
            "select id::text from planner.task where series_id=?::uuid and status='PLANNED'",
            String.class,
            id
        );
    }

    int activeCount(String id) {
        return jdbc.queryForObject(
            "select count(*) from planner.task where series_id=?::uuid and status='PLANNED'",
            Integer.class,
            id
        );
    }

    int events(UUID owner) {
        return jdbc.queryForObject(
            "select count(*) from planner.outbox_event where payload->>'userId'=?",
            Integer.class,
            owner.toString()
        );
    }

    @Test
    void mondayAnchorSurvivesRolloverSkipAndConflictingReopen() throws Exception {
        UUID owner = UUID.randomUUID();
        String s = series(owner, "GENERAL", "2026-09-07", null, MONDAY),
            first = active(s);
        var initial = body(call(owner, "GET", "/tasks/" + first, null));
        assertThat(initial.get("scheduledDate").asText()).isEqualTo("2026-09-09");
        assertThat(initial.get("occurrenceDate").asText()).isEqualTo("2026-09-07");
        assertThat(initial.get("note").asText()).isEqualTo("유지할 메모");
        assertThat(
            call(
                owner,
                "POST",
                "/tasks/" + first + "/complete",
                "{\"version\":0}"
            ).statusCode()
        ).isEqualTo(200);
        String second = active(s);
        assertThat(
            body(call(owner, "GET", "/tasks/" + second, null))
                .get("scheduledDate")
                .asText()
        ).isEqualTo("2026-09-14");
        int count = events(owner);
        assertThat(
            call(owner, "POST", "/tasks/" + first + "/reopen", "{\"version\":1}").statusCode()
        ).isEqualTo(409);
        assertThat(events(owner)).isEqualTo(count);
        seriesService.advance(
            owner,
            UUID.fromString(s),
            java.time.LocalDate.parse("2026-09-07"),
            "COMPLETED"
        );
        assertThat(active(s)).isEqualTo(second);
        assertThat(
            call(owner, "POST", "/tasks/" + second + "/skip", "{\"version\":0}").statusCode()
        ).isEqualTo(200);
        assertThat(
            body(call(owner, "GET", "/tasks/" + active(s), null))
                .get("scheduledDate")
                .asText()
        ).isEqualTo("2026-09-21");
        assertThat(
            body(call(owner, "GET", "/tasks/" + second, null))
                .get("status")
                .asText()
        ).isEqualTo("SKIPPED");
        assertThat(activeCount(s)).isOne();
    }

    @Test
    void rolloverIsOwnerScopedAndConcurrentIdempotentAndGetNeverMoves() throws Exception {
        UUID owner = UUID.randomUUID(),
            other = UUID.randomUUID();
        List<String> ids = new ArrayList<>();
        for (String status : List.of("PLANNED", "COMPLETED", "SKIPPED", "DELETED")) {
            String id = UUID.randomUUID().toString();
            ids.add(id);
            jdbc.update(
                "insert into planner.task(id,user_id,title,task_type,scheduled_date,status,version) values (?::uuid,?,'과거','GENERAL','2026-09-07',?,0)",
                id,
                owner,
                status
            );
        }
        assertThat(
            body(call(owner, "GET", "/calendar/2026-09-07", null))
                .get("tasks")
                .size()
        ).isEqualTo(3);
        assertThat(call(other, "POST", "/tasks/rollover", null).statusCode()).isEqualTo(200);
        assertThat(
            body(call(owner, "GET", "/tasks/" + ids.get(0), null))
                .get("scheduledDate")
                .asText()
        ).isEqualTo("2026-09-07");
        try (var pool = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var a = pool.submit(() -> call(owner, "POST", "/tasks/rollover", null));
            var b = pool.submit(() -> call(owner, "POST", "/tasks/rollover", null));
            var ra = a.get();
            var rb = b.get();
            assertThat(ra.statusCode()).as(ra.body()).isEqualTo(200);
            assertThat(rb.statusCode()).as(rb.body()).isEqualTo(200);
            assertThat(
                body(ra).get("movedCount").asInt() + body(rb).get("movedCount").asInt()
            ).isOne();
        }
        var moved = body(call(owner, "GET", "/tasks/" + ids.get(0), null));
        assertThat(moved.get("taskId").asText()).isEqualTo(ids.get(0));
        assertThat(moved.get("scheduledDate").asText()).isEqualTo("2026-09-09");
        assertThat(
            body(call(owner, "POST", "/tasks/rollover", null))
                .get("movedCount")
                .asInt()
        ).isZero();
        assertThat(
            body(call(owner, "GET", "/calendar/2026-09-07", null))
                .get("tasks")
                .size()
        ).isEqualTo(2);
        assertThat(events(owner)).isOne();
        assertThat(
            jdbc.queryForObject(
                "select payload->>'occurredAt' from planner.outbox_event where aggregateid=?",
                String.class,
                ids.get(0)
            )
        ).startsWith("2026-09-08T15:00:00");
    }

    @Test
    void concurrentCompleteSkipAndReopenKeepOneActiveAndRollbackLoser() throws Exception {
        UUID owner = UUID.randomUUID();
        String s = series(owner, "GENERAL", "2026-09-09", null, DAILY),
            id = active(s);
        try (var pool = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var a = pool.submit(() ->
                call(owner, "POST", "/tasks/" + id + "/complete", "{\"version\":0}")
            );
            var b = pool.submit(() ->
                call(owner, "POST", "/tasks/" + id + "/skip", "{\"version\":0}")
            );
            assertThat(
                List.of(a.get().statusCode(), b.get().statusCode())
            ).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(activeCount(s)).isOne();
        assertThat(events(owner)).isEqualTo(4);
        String next = active(s);
        assertThat(
            call(owner, "DELETE", "/tasks/" + next + "?version=0", null).statusCode()
        ).isEqualTo(204);
        try (var pool = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var a = pool.submit(() ->
                call(owner, "POST", "/tasks/" + id + "/reopen", "{\"version\":1}")
            );
            var b = pool.submit(() ->
                call(owner, "POST", "/tasks/" + id + "/reopen", "{\"version\":1}")
            );
            assertThat(
                List.of(a.get().statusCode(), b.get().statusCode())
            ).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(active(s)).isEqualTo(id);
        assertThat(events(owner)).isEqualTo(6);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            jdbc.update(
                "insert into planner.task(id,user_id,title,task_type,scheduled_date,status,version,series_id,occurrence_date) values(gen_random_uuid(),?,'충돌','GENERAL','2026-09-09','PLANNED',0,?::uuid,'2026-09-09')",
                owner,
                s
            )
        ).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(activeCount(s)).isOne();
    }

    @Test
    void archivedEndedPartialAndOneOffNeverCreateExtraOccurrences() throws Exception {
        UUID owner = UUID.randomUUID();
        String s = series(owner, "WORKOUT", "2026-09-09", null, DAILY),
            id = active(s);
        assertThat(
            call(owner, "POST", "/tasks/" + id + "/complete", "{\"version\":0}").statusCode()
        ).isEqualTo(409);
        assertThat(
            call(owner, "POST", "/series/" + s + "/archive", "{\"version\":0}").statusCode()
        ).isEqualTo(200);
        assertThat(active(s)).isEqualTo(id);
        assertThat(
            call(owner, "POST", "/tasks/" + id + "/skip", "{\"version\":0}").statusCode()
        ).isEqualTo(200);
        assertThat(activeCount(s)).isZero();
        assertThat(
            call(owner, "POST", "/tasks/" + id + "/reopen", "{\"version\":1}").statusCode()
        ).isEqualTo(200);
        assertThat(active(s)).isEqualTo(id);
        String ended = series(owner, "GENERAL", "2026-09-09", "2026-09-09", DAILY),
            eid = active(ended);
        assertThat(
            call(owner, "POST", "/tasks/" + eid + "/complete", "{\"version\":0}").statusCode()
        ).isEqualTo(200);
        assertThat(activeCount(ended)).isZero();
        seriesService.advance(
            owner,
            UUID.fromString(ended),
            java.time.LocalDate.parse("2026-09-07"),
            "PARTIAL"
        );
        assertThat(activeCount(ended)).isZero();
        String one = body(
            call(
                owner,
                "POST",
                "/tasks",
                "{\"title\":\"한 번\",\"taskType\":\"STUDY\",\"scheduledDate\":\"2026-09-07\"}"
            )
        )
            .get("taskId")
            .asText();
        assertThat(
            call(owner, "POST", "/tasks/" + one + "/skip", "{\"version\":0}").statusCode()
        ).isEqualTo(200);
        assertThat(
            call(owner, "POST", "/tasks/" + one + "/reopen", "{\"version\":1}").statusCode()
        ).isEqualTo(200);
        assertThat(
            body(call(owner, "GET", "/tasks/" + one, null))
                .get("scheduledDate")
                .asText()
        ).isEqualTo("2026-09-09");
    }

    @Test
    void failureWritingNextOccurrenceEventRollsBackEverything() throws Exception {
        UUID owner = UUID.randomUUID();
        String s = series(owner, "GENERAL", "2026-09-09", null, DAILY),
            id = active(s);
        jdbc.execute(
            "alter table planner.outbox_event add constraint reject_next_event check (not (payload->>'userId'='" +
                owner +
                "' and payload->>'type'='TASK_SCHEDULED')) not valid"
        );
        try {
            assertThat(
                call(
                    owner,
                    "POST",
                    "/tasks/" + id + "/complete",
                    "{\"version\":0}"
                ).statusCode()
            ).isEqualTo(500);
            assertThat(active(s)).isEqualTo(id);
            assertThat(events(owner)).isEqualTo(2);
            assertThat(
                jdbc.queryForObject(
                    "select count(*) from planner.task where series_id=?::uuid",
                    Integer.class,
                    s
                )
            ).isOne();
            assertThat(
                body(call(owner, "GET", "/tasks/" + id, null))
                    .get("version")
                    .asLong()
            ).isZero();
        } finally {
            jdbc.execute("alter table planner.outbox_event drop constraint reject_next_event");
        }
    }

    @Test
    void seriesEditKeepsAnchorAndExistingTaskButChangesNextAndEnforcesOwnerVersion()
        throws Exception {
        UUID owner = UUID.randomUUID(),
            other = UUID.randomUUID();
        String s = series(owner, "GENERAL", "2026-09-07", null, MONDAY),
            id = active(s);
        String update =
            "{\"title\":\"새 반복\",\"note\":\"새 메모\",\"rule\":" +
            DAILY +
            ",\"version\":0}";
        assertThat(call(other, "GET", "/series/" + s, null).statusCode()).isEqualTo(404);
        assertThat(call(other, "PATCH", "/series/" + s, update).statusCode()).isEqualTo(404);
        assertThat(
            call(other, "POST", "/series/" + s + "/archive", "{\"version\":0}").statusCode()
        ).isEqualTo(404);
        assertThat(call(owner, "PATCH", "/series/" + s, update).statusCode()).isEqualTo(200);
        assertThat(call(owner, "PATCH", "/series/" + s, update).statusCode()).isEqualTo(409);
        assertThat(
            call(owner, "POST", "/series/" + s + "/archive", "{\"version\":0}").statusCode()
        ).isEqualTo(409);
        var original = body(call(owner, "GET", "/tasks/" + id, null));
        assertThat(original.get("title").asText()).isEqualTo("반복");
        assertThat(original.get("occurrenceDate").asText()).isEqualTo("2026-09-07");
        assertThat(
            call(
                owner,
                "PATCH",
                "/tasks/" + id,
                "{\"title\":\"반복\",\"scheduledDate\":\"2026-09-07\",\"version\":0}"
            ).statusCode()
        ).isEqualTo(200);
        assertThat(
            body(call(owner, "POST", "/tasks/rollover", null))
                .get("movedCount")
                .asInt()
        ).isOne();
        var rolled = body(call(owner, "GET", "/tasks/" + id, null));
        assertThat(rolled.get("taskId").asText()).isEqualTo(id);
        assertThat(rolled.get("occurrenceDate").asText()).isEqualTo("2026-09-07");
        assertThat(rolled.get("scheduledDate").asText()).isEqualTo("2026-09-09");
        assertThat(
            call(owner, "POST", "/tasks/" + id + "/complete", "{\"version\":2}").statusCode()
        ).isEqualTo(200);
        var next = body(call(owner, "GET", "/tasks/" + active(s), null));
        assertThat(next.get("title").asText()).isEqualTo("새 반복");
        assertThat(next.get("note").asText()).isEqualTo("새 메모");
        assertThat(next.get("occurrenceDate").asText()).isEqualTo("2026-09-09");
        int n = events(owner);
        assertThat(
            call(
                owner,
                "PATCH",
                "/series/" + s,
                update.replace("\"version\":0", "\"version\":1")
            ).statusCode()
        ).isEqualTo(200);
        assertThat(events(owner)).isEqualTo(n);
    }

    @Test
    void invalidSeriesRulesDoNotWriteAnything() throws Exception {
        UUID owner = UUID.randomUUID();
        for (String rule : List.of(
            DAILY.replace("\"interval\":2", "\"interval\":0"),
            DAILY.replace("\"interval\":2", "\"interval\":366"),
            MONDAY.replace("[1]", "[]"),
            MONDAY.replace("[1]", "[8]"),
            DAILY.replace("\"monthDay\":1", "\"monthDay\":32")
        )) {
            assertThat(
                call(
                    owner,
                    "POST",
                    "/series",
                    "{\"title\":\"반복\",\"taskType\":\"GENERAL\",\"startDate\":\"2026-09-09\",\"rule\":" +
                        rule +
                        "}"
                ).statusCode()
            ).isEqualTo(400);
        }
        assertThat(
            call(
                owner,
                "POST",
                "/series",
                "{\"title\":\"반복\",\"taskType\":\"GENERAL\",\"startDate\":\"2026-09-09\",\"endDate\":\"2026-09-08\",\"rule\":" +
                    DAILY +
                    "}"
            ).statusCode()
        ).isEqualTo(400);
        assertThat(events(owner)).isZero();
    }

    @Test
    void databaseUniqueCollisionReturns409AndRollsBackCompletedTaskAndEvents()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String s = series(owner, "GENERAL", "2026-09-09", null, DAILY),
            id = active(s);
        jdbc.execute(
            """
            create function planner.inject_active_test() returns trigger language plpgsql as $$
            begin
              if pg_trigger_depth()=1 and new.series_id='%s'::uuid then
                insert into planner.task(id,user_id,title,task_type,scheduled_date,status,version,series_id,occurrence_date)
                  values(gen_random_uuid(),new.user_id,'충돌',new.task_type,new.scheduled_date,'PLANNED',0,new.series_id,new.occurrence_date);
              end if;
              return new;
            end $$
            """.formatted(s)
        );
        jdbc.execute(
            "create trigger inject_active_test before insert on planner.task for each row execute function planner.inject_active_test()"
        );
        try {
            var response = call(
                owner,
                "POST",
                "/tasks/" + id + "/complete",
                "{\"version\":0}"
            );
            assertThat(response.statusCode()).as(response.body()).isEqualTo(409);
            assertThat(active(s)).isEqualTo(id);
            assertThat(events(owner)).isEqualTo(2);
            assertThat(
                jdbc.queryForObject(
                    "select count(*) from planner.task where series_id=?::uuid",
                    Integer.class,
                    s
                )
            ).isOne();
        } finally {
            jdbc.execute("drop trigger inject_active_test on planner.task");
            jdbc.execute("drop function planner.inject_active_test()");
        }
    }

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
        r.add(
            "spring.datasource.hikari.connection-init-sql",
            () -> "set search_path to planner"
        );
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
    void recurringSeriesCreatesOnlyOneActiveOccurrence() throws Exception {
        UUID owner = UUID.randomUUID();
        var created = call(
            owner,
            "POST",
            "/series",
            """
            {"title":"반복","taskType":"GENERAL","startDate":"2099-01-01",
            "rule":{"frequency":"DAILY","interval":2,"weekdays":[],"monthDay":1}}
            """
        );
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
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
        assertThat(
            call(owner, "POST", "/tasks/" + id + "/reopen", "{\"version\":0}").statusCode()
        ).isEqualTo(409);
        assertThat(
            call(other, "POST", "/tasks/" + id + "/complete", "{\"version\":0}").statusCode()
        ).isEqualTo(404);
        assertThat(
            call(other, "DELETE", "/tasks/" + id + "?version=0", null).statusCode()
        ).isEqualTo(404);
        assertThat(
            call(
                other,
                "PATCH",
                "/tasks/" + id,
                "{\"title\":\"침범\",\"scheduledDate\":\"2026-12-31\",\"version\":0}"
            ).statusCode()
        ).isEqualTo(404);
        assertThat(
            call(owner, "POST", "/tasks/" + id + "/complete", "{\"version\":0}").statusCode()
        ).isEqualTo(200);
        assertThat(
            call(owner, "POST", "/tasks/" + id + "/complete", "{\"version\":1}").statusCode()
        ).isEqualTo(409);
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
        assertThat(
            body(call(owner, "GET", "/calendar/2027-01-01", null))
                .get("tasks")
                .size()
        ).isZero();
        assertThat(
            body(call(owner, "GET", "/calendar?from=2027-01-01&to=2027-01-01", null))
                .get("days")
                .get(0)
                .get("totalCount")
                .asInt()
        ).isZero();
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
        assertThat(call(owner, "GET", "/calendar/2026-02-30", null).statusCode()).isEqualTo(
            400
        );
        assertThat(
            call(
                owner,
                "POST",
                "/tasks",
                "{\"title\":\"날짜 오류\",\"taskType\":\"GENERAL\",\"scheduledDate\":\"2026-02-30\"}"
            ).statusCode()
        ).isEqualTo(400);
        assertThat(
            call(owner, "GET", "/calendar?from=2026-09-08&to=2026-09-07", null).statusCode()
        ).isEqualTo(400);
        var days = body(
            call(owner, "GET", "/calendar?from=2026-09-07&to=2026-09-08", null)
        ).get("days");
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
                call(
                    owner,
                    "POST",
                    "/tasks/" + id + "/complete",
                    "{\"version\":0}"
                ).statusCode()
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
    void projectionsUseOneQueryFor100And1000TasksAndKeepExactCategoryCounts()
        throws Exception {
        var stats = entityManagerFactory
            .unwrap(org.hibernate.SessionFactory.class)
            .getStatistics();
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
            var summary = body(
                call(owner, "GET", "/calendar?from=2026-09-01&to=2026-10-12", null)
            );
            assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
            assertThat(summary.get("days").size()).isEqualTo(42);
            var target = summary.get("days").get(6);
            assertThat(target.get("totalCount").asInt()).isEqualTo(size);
            assertThat(target.get("completedCount").asInt()).isEqualTo(size / 4);
            assertThat(
                target.get("categoryProgress").get(0).get("totalCount").asInt()
            ).isEqualTo(size / 2);
            assertThat(
                target.get("categoryProgress").get(0).get("completedCount").asInt()
            ).isEqualTo(size / 4);
            assertThat(
                target.get("categoryProgress").get(1).get("taskType").asText()
            ).isEqualTo("WORKOUT");
            assertThat(
                target.get("categoryProgress").get(2).get("taskType").asText()
            ).isEqualTo("STUDY");
            assertThat(
                target.get("categoryProgress").get(3).get("taskType").asText()
            ).isEqualTo("CLIMBING");
            assertThat(
                body(call(UUID.randomUUID(), "GET", "/calendar/2026-09-07", null))
                    .get("tasks")
                    .size()
            ).isZero();
        }
    }
}
