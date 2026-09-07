package io.todorok.messaging.integration;

import static org.assertj.core.api.Assertions.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import io.todorok.activity.ActivityApplication;
import io.todorok.planner.PlannerApplication;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

/** Actual HTTP -> service DB -> Debezium -> Kafka -> service listener -> ack -> HTTP. */
class ActivityServiceRoundTripTest {

    @Test
    void templateServiceOutageAndLostApprovalResponseRecoverWithTheOriginalCommand() throws Exception {
        UUID owner=UUID.randomUUID(), command=UUID.randomUUID();
        String templateId=ok(call("activity",owner,"POST","/templates",JSON.writeValueAsString(Map.of("commandId",UUID.randomUUID(),
            "name","응답 복구","domain","WORKOUT","kind","FREE_WORKOUT","fields",List.of()))),201).path("templateId").asText();
        String body=JSON.writeValueAsString(Map.of("commandId",command,"title","복구 일정","taskType","WORKOUT","scheduledDate","2026-09-07",
            "templateSelection",Map.of("templateId",templateId,"expectedTemplateVersion",1)));
        var proxy=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var forward=new java.util.concurrent.atomic.AtomicBoolean(false);
        String real="http://localhost:"+activity.getEnvironment().getProperty("local.server.port")+"/api/activity/v1";
        proxy.createContext("/api/activity/v1/internal/template-selections",exchange->{
            try {
                if(forward.get()) HTTP.send(HttpRequest.newBuilder(URI.create(real+"/internal/template-selections"))
                    .header("Authorization",exchange.getRequestHeaders().getFirst("Authorization")).header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(exchange.getRequestBody().readAllBytes())).build(),HttpResponse.BodyHandlers.discarding());
                // Deliberately lose every approval response after the backend commits.
            } catch(Exception failure) { throw new java.io.IOException(failure); }
            finally { exchange.close(); }
        });
        proxy.start();
        planner.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("binding-fault",
            Map.of("todorok.template-service.base-url","http://127.0.0.1:"+proxy.getAddress().getPort()+"/api/activity/v1")));
        try {
            assertThat(ok(call("planner",owner,"POST","/tasks",body),503).path("retryable").asBoolean()).isTrue();
            assertThat(infra.database().queryForObject("select count(*) from activity.template_selection_binding where user_id=?",Integer.class,owner)).isZero();
            forward.set(true);
            ok(call("planner",owner,"POST","/tasks",body),503);
            assertThat(infra.database().queryForObject("select count(*) from activity.template_selection_binding where user_id=?",Integer.class,owner)).isOne();
            assertThat(infra.database().queryForObject("select count(*) from planner.task where user_id=?",Integer.class,owner)).isZero();
        } finally { planner.getEnvironment().getPropertySources().remove("binding-fault"); proxy.stop(0); }
        var recovered=ok(call("planner",owner,"POST","/tasks",body),201);
        assertThat(ok(call("planner",owner,"POST","/tasks",body),201)).isEqualTo(recovered);
        assertThat(infra.database().queryForObject("select count(*) from planner.task where user_id=?",Integer.class,owner)).isOne();
        assertThat(infra.database().queryForObject("select count(*) from planner.outbox_event where aggregateid=?",Integer.class,recovered.path("taskId").asText())).isOne();
    }

    @Test
    void templateBindingRecoversApprovalAndPlannerFailuresAndPropagatesSeriesThroughKafka() throws Exception {
        UUID owner=UUID.randomUUID(), command=UUID.randomUUID();
        var template=ok(call("activity",owner,"POST","/templates",JSON.writeValueAsString(Map.of("commandId",UUID.randomUUID(),
            "name","선택 공부","domain","STUDY","kind","STUDY_CATEGORY","fields",List.of()))),201);
        String templateId=template.path("templateId").asText();
        var request=new LinkedHashMap<String,Object>(Map.of("commandId",command,"title","연결 일정","taskType","STUDY",
            "scheduledDate","2026-09-07","templateSelection",Map.of("templateId",templateId,"expectedTemplateVersion",1)));
        String body=JSON.writeValueAsString(request);
        // Inject final planner transaction failure after real Activity HTTP approval.
        infra.database().execute("alter table planner.outbox_event add constraint fail_binding_test check(payload->>'userId'<>'"+owner+"') not valid");
        try { ok(call("planner",owner,"POST","/tasks",body),500); }
        finally { infra.database().execute("alter table planner.outbox_event drop constraint fail_binding_test"); }
        assertThat(infra.database().queryForObject("select count(*) from activity.template_selection_binding where user_id=?",Integer.class,owner)).isOne();
        assertThat(infra.database().queryForObject("select count(*) from planner.task where user_id=?",Integer.class,owner)).isZero();
        String selected=infra.database().queryForObject("select id::text from activity.template_selection_binding where user_id=?",String.class,owner);
        try (var pool=Executors.newFixedThreadPool(4)) {
            var jobs=new ArrayList<Future<JsonNode>>();
            for(int i=0;i<4;i++) jobs.add(pool.submit(()->ok(call("planner",owner,"POST","/tasks",body),201)));
            var first=jobs.getFirst().get();
            for(var job:jobs) assertThat(job.get()).isEqualTo(first);
            assertThat(first.path("templateLink").path("bindingId").asText()).isEqualTo(selected);
        }
        assertThat(infra.database().queryForObject("select count(*) from planner.task where user_id=?",Integer.class,owner)).isOne();
        String task=infra.database().queryForObject("select id::text from planner.task where user_id=?",String.class,owner);
        assertThat(infra.database().queryForObject("select count(*) from planner.outbox_event where aggregateid=?",Integer.class,task)).isOne();
        var queued=JSON.readTree(infra.database().queryForObject("select payload::text from planner.outbox_event where aggregateid=?",String.class,task));
        assertThat(queued.path("version").asInt()).isEqualTo(2);
        assertThat(queued.path("payload").path("templateLink").path("bindingId").asText()).isEqualTo(selected);
        try {
            var delivered=infra.consume("todorok.task.v1",UUID.fromString(queued.path("eventId").asText()),Duration.ofSeconds(15));
            assertThat(delivered.value()).as("CDC must preserve the v2 payload, including explicit null identity members").isEqualTo(queued);
        }
        catch(AssertionError absent) { infra.awaitConnectorRunning(); throw absent; }
        await(()->infra.database().queryForObject("select count(*) from activity.task_reference where task_id=? and template_binding_id is not null",Integer.class,UUID.fromString(task))==1);
        assertThat(ok(call("activity",owner,"GET","/tasks/"+task+"/record-template",null),200).path("linked").asBoolean()).isTrue();
        var originalEvent=(tools.jackson.databind.node.ObjectNode)JSON.readTree(infra.database().queryForObject(
            "select payload::text from planner.outbox_event where aggregateid=?",String.class,task));
        send("todorok.task.v1",task,originalEvent.toString());
        var legacy=originalEvent.deepCopy().put("eventId",UUID.randomUUID().toString()).put("version",1).put("aggregateVersion",8);
        ((tools.jackson.databind.node.ObjectNode)legacy.get("payload")).remove("templateLink");
        ((tools.jackson.databind.node.ObjectNode)legacy.get("payload")).remove("seriesId");
        send("todorok.task.v1",task,legacy.toString());
        await(()->infra.database().queryForObject("select count(*) from activity.processed_event where event_id=?",Integer.class,UUID.fromString(legacy.path("eventId").asText()))==1);
        send("todorok.task.v1",task,originalEvent.deepCopy().put("eventId",UUID.randomUUID().toString()).toString());
        assertThat(ok(call("activity",owner,"GET","/tasks/"+task+"/record-template",null),200).path("templateLink").path("bindingId").asText()).isEqualTo(selected);
        UUID invalidTask=UUID.randomUUID(),invalidEvent=UUID.randomUUID();
        var forged=originalEvent.deepCopy().put("eventId",invalidEvent.toString());
        ((tools.jackson.databind.node.ObjectNode)forged.get("payload")).put("taskId",invalidTask.toString());
        send("todorok.task.v1",invalidTask.toString(),forged.toString());
        infra.consume("todorok.task.v1.dlt",invalidEvent,Duration.ofSeconds(45));
        assertThat(infra.database().queryForObject("select count(*) from activity.task_reference where task_id=?",Integer.class,invalidTask)).isZero();
        assertThat(infra.database().queryForObject("select count(*) from activity.processed_event where event_id=?",Integer.class,invalidEvent)).isZero();
        var updated=ok(call("planner",owner,"PATCH","/tasks/"+task,JSON.writeValueAsString(Map.of("title","이월 연결",
            "scheduledDate",LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1).toString(),"version",0))),200);
        assertThat(updated.path("templateLink").path("bindingId").asText()).isEqualTo(selected);
        ok(call("planner",owner,"POST","/tasks/rollover","{}"),200);
        assertThat(ok(call("planner",owner,"GET","/tasks/"+task,null),200).path("templateLink")).isEqualTo(updated.path("templateLink"));
        assertThat(ok(call("activity",owner,"POST","/activities",request(task,"STUDY","COMPLETED",UUID.randomUUID(),"{}")),409)
            .path("code").asText()).isEqualTo("TEMPLATE_RECORD_NOT_READY");
        request.put("title","다른 요청");
        assertThat(ok(call("planner",owner,"POST","/tasks",JSON.writeValueAsString(request)),409).path("code").asText()).isEqualTo("COMMAND_REUSE");
        Object selection=request.remove("templateSelection");
        assertThat(ok(call("planner",owner,"POST","/tasks",JSON.writeValueAsString(request)),409).path("code").asText()).isEqualTo("COMMAND_REUSE");
        request.put("templateSelection",selection);
        UUID seriesCommand=UUID.randomUUID();
        String seriesBody=JSON.writeValueAsString(Map.of("commandId",seriesCommand,"title","연결 반복","taskType","STUDY","startDate","2026-09-07",
            "rule",Map.of("frequency","DAILY","interval",1,"weekdays",List.of(),"monthDay",1),
            "templateSelection",Map.of("templateId",templateId,"expectedTemplateVersion",1)));
        JsonNode series;
        try(var pool=Executors.newFixedThreadPool(4)) {
            var jobs=new ArrayList<Future<JsonNode>>();
            for(int i=0;i<4;i++) jobs.add(pool.submit(()->ok(call("planner",owner,"POST","/series",seriesBody),201)));
            series=jobs.getFirst().get();
            for(var job:jobs) assertThat(job.get()).isEqualTo(series);
        }
        assertThat(ok(call("planner",owner,"POST","/series",seriesBody),201)).isEqualTo(series);
        UUID seriesId=UUID.fromString(series.path("seriesId").asText());
        String firstTask=infra.database().queryForObject("select id::text from planner.task where series_id=?",String.class,seriesId);
        ok(call("activity",owner,"POST","/templates/"+templateId+"/versions",JSON.writeValueAsString(Map.of("commandId",UUID.randomUUID(),
            "expectedRevision",0,"name","새 기록 정의","fields",List.of()))),201);
        var current=ok(call("activity",owner,"GET","/tasks/"+task+"/record-template",null),200);
        assertThat(current.path("template").path("currentVersion").path("templateVersion").asLong()).isEqualTo(2);
        assertThat(current.path("templateLink").path("selectedTemplateVersion").asLong()).isEqualTo(1);
        ok(call("activity",owner,"POST","/templates/"+templateId+"/archive",JSON.writeValueAsString(Map.of("commandId",UUID.randomUUID(),"expectedRevision",1))),200);
        ok(call("planner",owner,"POST","/tasks/"+firstTask+"/skip","{\"version\":0}"),200);
        assertThat(infra.database().queryForObject("select count(*) from planner.task where series_id=?",Integer.class,seriesId)).isEqualTo(2);
        var next=infra.database().queryForMap("select id,template_binding_id from planner.task where series_id=? and status='PLANNED'",seriesId);
        assertThat(next.get("template_binding_id").toString()).isEqualTo(series.path("templateLink").path("bindingId").asText());
        await(()->infra.database().queryForObject("select count(*) from activity.task_reference where task_id=? and template_binding_id is not null",Integer.class,next.get("id"))==1);
        assertThat(ok(call("activity",owner,"GET","/tasks/"+next.get("id")+"/record-template",null),200).path("template").path("archived").asBoolean()).isTrue();
        // Exercise the established completion-event boundary; typed record writes remain deliberately gated until09B2.
        UUID completedId=UUID.randomUUID();
        var completed=JSON.createObjectNode().put("eventId",UUID.randomUUID().toString()).put("type","ACTIVITY_COMPLETED")
            .put("version",1).put("aggregateVersion",0).put("occurredAt",Instant.now().toString()).put("userId",owner.toString());
        completed.putObject("payload").put("activityId",completedId.toString()).put("taskId",next.get("id").toString())
            .put("activityType","STUDY").put("completedAt","2026-09-01T01:00:00Z").put("outcome","과거 완료 경계");
        send("todorok.activity.v1",completedId.toString(),completed.toString());
        await(()->infra.database().queryForObject("select count(*) from planner.task where series_id=?",Integer.class,seriesId)==3);
        assertThat(infra.database().queryForObject("select count(distinct template_binding_id) from planner.task where series_id=?",Integer.class,seriesId)).isOne();
        send("todorok.activity.v1",completedId.toString(),completed.toString());
        var correctionEvent=completed.deepCopy().put("eventId",UUID.randomUUID().toString()).put("type","ACTIVITY_CORRECTED").put("aggregateVersion",1);
        ((tools.jackson.databind.node.ObjectNode)correctionEvent.get("payload")).put("completionStatus","COMPLETED").put("previousPerformedAt","2026-09-01T01:00:00Z");
        send("todorok.activity.v1",completedId.toString(),correctionEvent.toString());
        await(()->infra.database().queryForObject("select count(*) from planner.activity_completion_result where activity_id=? and revision=1",Integer.class,completedId)==1);
        assertThat(infra.database().queryForObject("select count(*) from planner.task where series_id=?",Integer.class,seriesId)).isEqualTo(3);
        request.put("commandId",UUID.randomUUID());
        assertThat(ok(call("planner",owner,"POST","/tasks",JSON.writeValueAsString(request)),409).path("code").asText()).isEqualTo("TEMPLATE_ARCHIVED");
    }

    static String correction(long version, String detail) {
        return "{\"expectedVersion\":" + version + ",\"performedAt\":\"2026-08-31T10:00:00+09:00\",\"note\":\"수정 메모\",\"detail\":" + detail + "}";
    }

    @Test
    void pastRecordsRespectActiveArchivedPastFutureAndPartialSeriesPolicy() throws Exception {
        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        var past = today.minusDays(14);
        for (String scenario : List.of("PAST", "FUTURE", "ARCHIVED", "ENDED", "PARTIAL")) {
            UUID owner = UUID.randomUUID();
            int interval = scenario.equals("FUTURE") ? 30 : 1;
            String body = "{\"title\":\"지난 반복\",\"taskType\":\"STUDY\",\"startDate\":\"" + past + "\","
                + (scenario.equals("ENDED") ? "\"endDate\":\"" + past + "\"," : "")
                + "\"rule\":{\"frequency\":\"DAILY\",\"interval\":" + interval + ",\"weekdays\":[],\"monthDay\":1}}";
            var series = ok(call("planner", owner, "POST", "/series", body), 201);
            String seriesId = series.path("seriesId").asText();
            UUID sid = UUID.fromString(seriesId);
            String task = infra.database().queryForObject("select id::text from planner.task where series_id=?", String.class, sid);
            await(() -> infra.database().queryForObject("select count(*) from activity.task_reference where task_id=?", Integer.class, UUID.fromString(task)) == 1);
            if (scenario.equals("ARCHIVED")) ok(call("planner", owner, "POST", "/series/" + seriesId + "/archive", "{\"version\":" + series.path("version").asLong() + "}"), 200);
            String status = scenario.equals("PARTIAL") ? "PARTIAL" : "COMPLETED";
            String id = ok(call("activity", owner, "POST", "/activities", request(task, "STUDY", status, UUID.randomUUID(), "{}")
                .replace("2026-09-08T01:00:00+09:00", past + "T10:00:00+09:00")), 201).path("activityId").asText();
            if (!status.equals("PARTIAL")) applied(owner, id);
            int expected = scenario.equals("PAST") || scenario.equals("FUTURE") ? 2 : 1;
            assertThat(infra.database().queryForObject("select count(*) from planner.task where series_id=?", Integer.class, sid)).as(scenario).isEqualTo(expected);
            if (expected == 2) {
                var next = infra.database().queryForMap("select id,scheduled_date,occurrence_date from planner.task where series_id=? and status='PLANNED'", sid);
                assertThat(next.get("occurrence_date").toString()).isEqualTo(past.plusDays(interval).toString());
                assertThat(next.get("scheduled_date").toString()).isEqualTo((scenario.equals("PAST") ? today : past.plusDays(interval)).toString());
                // A later past correction sees the existing active occurrence and must keep its ID and dates.
                ok(call("activity", owner, "PATCH", "/activities/" + id, correction(0, "{}")), 200);
                applied(owner, id);
                assertThat(infra.database().queryForMap("select id,scheduled_date,occurrence_date from planner.task where series_id=? and status='PLANNED'", sid)).isEqualTo(next);
                assertThat(infra.database().queryForObject("select count(*) from planner.task where series_id=?", Integer.class, sid)).isEqualTo(2);
            }
        }
    }

    @Test
    void correctionPreservesTypedHistoryAndVersionRacesAndVoid() throws Exception {
        UUID owner = UUID.randomUUID();
        for (String type : List.of("WORKOUT", "STUDY", "CLIMBING")) {
            String task = task(owner, type);
            String originalRequest = request(task, type, "COMPLETED", UUID.randomUUID(), "{}");
            String id = ok(call("activity", owner, "POST", "/activities",
                originalRequest), 201).get("activityId").asText();
            applied(owner, id);
            String detail = switch(type) {
                case "WORKOUT" -> "{\"workout\":{\"sets\":[{\"exercise\":\"스쿼트\",\"reps\":7,\"durationSeconds\":90}]}}";
                case "STUDY" -> "{\"study\":{\"subject\":\"수학\",\"durationMinutes\":30}}";
                default -> "{\"climbing\":{\"durationSeconds\":600,\"rounds\":[{\"grade\":\"V3\",\"attempts\":2}]}}";
            };
            String path = "/activities/" + id;
            ok(call("activity", UUID.randomUUID(), "PATCH", path, correction(0, detail)), 404);
            ok(call("activity", owner, "PATCH", path, correction(0, "{\"workout\":{},\"study\":{}}")), 400);
            ok(call("activity", owner, "PATCH", path, correction(0, detail).replace("\"note\"", "\"startedAt\":\"2026-08-31T10:00:00+09:00\",\"note\"")), 400);
            String timedCorrection = correction(0, detail).replace("\"note\"", "\"startedAt\":\"2026-08-31T10:00:00+09:00\",\"endedAt\":\"2026-08-31T11:00:00+09:00\",\"note\"");
            var changed = ok(call("activity", owner, "PATCH", path, timedCorrection), 200);
            assertThat(changed.path("version").asLong()).isEqualTo(1);
            assertThat(changed.path("previousPerformedAt").asText()).startsWith("2026-09-07T16:");
            assertThat(changed.path("detail").path(type.toLowerCase()).isObject()).isTrue();
            applied(owner, id);
            var replayed = ok(call("activity", owner, "POST", "/activities", originalRequest), 201);
            assertThat(replayed.path("activityId").asText()).isEqualTo(id);
            assertThat(replayed.path("version").asLong()).isEqualTo(1);
            assertThat(replayed.path("note").asText()).isEqualTo("수정 메모");
            assertThat(ok(call("planner", owner, "GET", "/tasks/" + task, null), 200).path("scheduledDate").asText()).isEqualTo("2026-08-31");
            assertThat(OffsetDateTime.parse(ok(call("planner", owner, "GET", "/tasks/" + task, null), 200).path("startedAt").asText()).toInstant()).isEqualTo(Instant.parse("2026-08-31T01:00:00Z"));
            assertThat(infra.database().queryForObject("select snapshot->>'note' from activity.activity_revision_history where activity_id=? and revision=0", String.class, UUID.fromString(id))).isEqualTo("완료 메모");
            assertThatThrownBy(() -> infra.database().update("delete from activity.activity_revision_history where activity_id=?", UUID.fromString(id))).hasMessageContaining("immutable");
            ok(call("activity", owner, "PATCH", path, correction(0, detail)), 409);
            try (var pool = Executors.newFixedThreadPool(4)) {
                var jobs = new ArrayList<Future<Integer>>();
                for (int n = 0; n < 4; n++) jobs.add(pool.submit(() -> call("activity", owner, "PATCH", path, correction(1, detail)).statusCode()));
                var statuses = new ArrayList<Integer>();
                for (var job : jobs) statuses.add(job.get());
                assertThat(statuses).containsExactlyInAnyOrder(200, 409, 409, 409);
            }
            applied(owner, id);
            assertThat(ok(call("planner", owner, "GET", "/tasks/" + task, null), 200).hasNonNull("startedAt")).isFalse();
            ok(call("activity", owner, "POST", path + "/void", "{\"reason\":\"취소\",\"version\":2}"), 200);
            applied(owner, id);
            ok(call("activity", owner, "PATCH", path, correction(3, detail)), 409);
            assertThat(infra.database().queryForObject("select count(*) from activity.activity_revision_history where activity_id=?", Integer.class, UUID.fromString(id))).isEqualTo(3);
            assertThat(ok(call("activity", owner, "GET", path, null), 200).path("detail")).isEqualTo(changed.path("detail"));
        }
    }

    @Test
    void correctionRollbackPartialAndStoppedConsumerRecovery() throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "STUDY");
        String id = ok(call("activity", owner, "POST", "/activities", request(task, "STUDY", "COMPLETED", UUID.randomUUID(), "{}")), 201).path("activityId").asText();
        applied(owner, id);
        String path = "/activities/" + id;
        infra.database().execute("alter table activity.outbox_event add constraint fail_correction check(type<>'ACTIVITY_CORRECTED') not valid");
        try { ok(call("activity", owner, "PATCH", path, correction(0, "{\"study\":{\"durationMinutes\":45}}")), 500); }
        finally { infra.database().execute("alter table activity.outbox_event drop constraint fail_correction"); }
        assertThat(ok(call("activity", owner, "GET", path, null), 200).path("version").asLong()).isZero();
        assertThat(infra.database().queryForObject("select count(*) from activity.activity_revision_history where activity_id=?", Integer.class, UUID.fromString(id))).isZero();
        assertThat(infra.database().queryForObject("select duration_minutes from activity.study_detail where activity_id=?", Integer.class, UUID.fromString(id))).isNull();
        var listener = planner.getBean(KafkaListenerEndpointRegistry.class).getListenerContainer("planner-activity");
        listener.stop();
        try {
            assertThat(ok(call("activity", owner, "PATCH", path, correction(0, "{}")), 200).path("syncState").asText()).isEqualTo("PENDING");
            assertThat(ok(call("activity", owner, "PATCH", path, correction(1, "{\"study\":{\"durationMinutes\":45}}")), 200).path("version").asLong()).isEqualTo(2);
            assertThat(ok(call("activity", owner, "GET", path, null), 200).path("syncState").asText()).isEqualTo("PENDING");
            String event = infra.database().queryForObject("select payload::text from activity.outbox_event where aggregateid=? and (payload->>'aggregateVersion')::bigint=2", String.class, id);
            UUID eventId = UUID.fromString(JSON.readTree(event).path("eventId").asText());
            infra.database().execute("alter table planner.outbox_event add constraint fail_correction_ack check(type<>'ACTIVITY_SYNC_RESULT') not valid");
            try {
                assertThatThrownBy(() -> planner.getBean(io.todorok.planner.task.ActivityCompletionConsumer.class).receive(event)).isInstanceOf(RuntimeException.class);
            } finally { infra.database().execute("alter table planner.outbox_event drop constraint fail_correction_ack"); }
            assertThat(infra.database().queryForObject("select count(*) from planner.processed_event where event_id=?", Integer.class, eventId)).isZero();
            assertThat(infra.database().queryForObject("select revision from planner.activity_completion_result where activity_id=?", Long.class, UUID.fromString(id))).isZero();
            assertThat(ok(call("planner", owner, "GET", "/tasks/" + task, null), 200).path("scheduledDate").asText()).isEqualTo("2026-09-08");
        } finally { listener.start(); }
        applied(owner, id);
        assertThat(ok(call("planner", owner, "GET", "/tasks/" + task, null), 200).path("completionSummary").asText()).contains("45분");
        String partialTask = task(owner, "STUDY");
        String partial = ok(call("activity", owner, "POST", "/activities", request(partialTask, "STUDY", "PARTIAL", UUID.randomUUID(), "{}")), 201).path("activityId").asText();
        assertThat(ok(call("activity", owner, "PATCH", "/activities/" + partial, correction(0, "{}")), 200).path("syncState").asText()).isEqualTo("NOT_REQUIRED");
        assertThat(infra.database().queryForObject("select count(*) from activity.outbox_event where aggregateid=?", Integer.class, partial)).isEqualTo(1);
        String partialEvent = infra.database().queryForObject("select id::text from activity.outbox_event where aggregateid=?", String.class, partial);
        await(() -> infra.database().queryForObject("select count(*) from planner.processed_event where event_id=?", Integer.class, UUID.fromString(partialEvent)) == 1);
        assertThat(ok(call("activity", owner, "GET", "/activities/" + partial, null), 200).path("syncState").asText()).isEqualTo("NOT_REQUIRED");
        assertThat(ok(call("planner", owner, "GET", "/tasks/" + partialTask, null), 200).path("status").asText()).isEqualTo("PLANNED");
    }

    @Test
    void correctionBeforeCompletionAndOldCorrectionAfterVoidUseRevisionTombstones() throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "STUDY");
        String id = UUID.randomUUID().toString();
        var correction = JSON.createObjectNode();
        correction.put("eventId", UUID.randomUUID().toString()).put("type", "ACTIVITY_CORRECTED").put("version", 1)
            .put("aggregateVersion", 2).put("occurredAt", Instant.now().toString()).put("userId", owner.toString());
        correction.putObject("payload").put("activityId", id).put("taskId", task).put("activityType", "STUDY")
            .put("completionStatus", "COMPLETED")
            .put("completedAt", "2026-08-31T01:00:00Z").put("previousPerformedAt", "2026-09-01T01:00:00Z").put("outcome", "최신 수정");
        send("todorok.activity.v1", id, correction.toString());
        await(() -> infra.database().queryForObject("select count(*) from planner.activity_completion_result where activity_id=? and revision=2", Integer.class, UUID.fromString(id)) == 1);
        send("todorok.activity.v1", id, correction.toString());
        correction.put("eventId", UUID.randomUUID().toString()).put("aggregateVersion", 1);
        ((tools.jackson.databind.node.ObjectNode) correction.get("payload")).put("outcome", "오래된 수정");
        send("todorok.activity.v1", id, correction.toString());
        var completion = correction.deepCopy().put("eventId", UUID.randomUUID().toString()).put("type", "ACTIVITY_COMPLETED").put("aggregateVersion", 0);
        ((tools.jackson.databind.node.ObjectNode) completion.get("payload")).remove("previousPerformedAt");
        ((tools.jackson.databind.node.ObjectNode) completion.get("payload")).remove("completionStatus");
        send("todorok.activity.v1", id, completion.toString());
        String last = completion.path("eventId").asText();
        await(() -> infra.database().queryForObject("select count(*) from planner.processed_event where event_id=?", Integer.class, UUID.fromString(last)) == 1);
        assertThat(ok(call("planner", owner, "GET", "/tasks/" + task, null), 200).path("completionSummary").asText()).isEqualTo("최신 수정");
        var voided = correction.deepCopy().put("eventId", UUID.randomUUID().toString()).put("type", "ACTIVITY_VOIDED").put("aggregateVersion", 3);
        voided.putObject("payload").put("activityId", id).put("taskId", task).put("voidedAt", Instant.now().toString()).put("reason", "취소");
        send("todorok.activity.v1", id, voided.toString());
        correction.put("eventId", UUID.randomUUID().toString());
        send("todorok.activity.v1", id, correction.toString());
        String finalId = correction.path("eventId").asText();
        await(() -> infra.database().queryForObject("select count(*) from planner.processed_event where event_id=?", Integer.class, UUID.fromString(finalId)) == 1);
        assertThat(ok(call("planner", owner, "GET", "/tasks/" + task, null), 200).path("status").asText()).isEqualTo("PLANNED");
    }

    @Test
    void correctionAndVoidRaceHasOneWinnerAndPreservesOneHistoryRevision() throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "WORKOUT");
        String id = ok(call("activity", owner, "POST", "/activities", request(task, "WORKOUT", "PARTIAL", UUID.randomUUID(), "{}")), 201).path("activityId").asText();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var patch = pool.submit(() -> call("activity", owner, "PATCH", "/activities/" + id, correction(0, "{}")).statusCode());
            var cancel = pool.submit(() -> call("activity", owner, "POST", "/activities/" + id + "/void", "{\"version\":0,\"reason\":\"취소\"}").statusCode());
            assertThat(List.of(patch.get(), cancel.get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(ok(call("activity", owner, "GET", "/activities/" + id, null), 200).path("version").asLong()).isEqualTo(1);
        assertThat(infra.database().queryForObject("select count(*) from activity.activity_revision_history where activity_id=?", Integer.class, UUID.fromString(id))).isEqualTo(1);
    }

    @Test
    void finiteSeriesActivityVoidAndRecompletionCannotDuplicateCompletedSuccessor()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String series = ok(
            call(
                "planner",
                owner,
                "POST",
                "/series",
                "{\"title\":\"유한 반복\",\"taskType\":\"STUDY\",\"startDate\":\"2026-09-07\",\"endDate\":\"2026-09-08\",\"rule\":{\"frequency\":\"DAILY\",\"interval\":1,\"weekdays\":[],\"monthDay\":1}}"
            ),
            201
        )
            .get("seriesId")
            .asText();
        String first = infra
            .database()
            .queryForObject(
                "select id::text from planner.task where series_id=? and status='PLANNED'",
                String.class,
                UUID.fromString(series)
            );
        await(
            () ->
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.task_reference where task_id=?",
                        Integer.class,
                        UUID.fromString(first)
                    ) == 1
        );
        String original = ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                request(first, "STUDY", "COMPLETED", UUID.randomUUID(), "{}")
            ),
            201
        )
            .get("activityId")
            .asText();
        applied(owner, original);
        String second = infra
            .database()
            .queryForObject(
                "select id::text from planner.task where series_id=? and status='PLANNED'",
                String.class,
                UUID.fromString(series)
            );
        await(
            () ->
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.task_reference where task_id=?",
                        Integer.class,
                        UUID.fromString(second)
                    ) == 1
        );
        String successor = ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                request(second, "STUDY", "COMPLETED", UUID.randomUUID(), "{}")
            ),
            201
        )
            .get("activityId")
            .asText();
        applied(owner, successor);
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities/" + original + "/void",
                "{\"reason\":\"수정 기록\",\"version\":0}"
            ),
            200
        );
        applied(owner, original);
        String revised = ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                request(first, "STUDY", "COMPLETED", UUID.randomUUID(), "{}")
            ),
            201
        )
            .get("activityId")
            .asText();
        applied(owner, revised);
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from planner.task where series_id=?",
                    Integer.class,
                    UUID.fromString(series)
                )
        ).isEqualTo(2);
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from planner.task where series_id=? and status='PLANNED'",
                    Integer.class,
                    UUID.fromString(series)
                )
        ).isZero();
        assertThat(
            ok(call("planner", owner, "GET", "/tasks/" + second, null), 200)
                .get("activityId")
                .asText()
        ).isEqualTo(successor);
        assertThat(
            ok(
                call("activity", owner, "GET", "/activities/" + original, null),
                200
            )
                .get("status")
                .asText()
        ).isEqualTo("VOIDED");
    }

    static MessagingInfrastructureFixture infra;
    static ConfigurableApplicationContext planner, activity;
    static final JsonMapper JSON = JsonMapper.builder()
        .findAndAddModules()
        .build();
    static final java.security.KeyPair KEYS = keys();
    static final java.security.KeyPair SERVICE_KEYS = keys();
    static final HttpClient HTTP = HttpClient.newHttpClient();

    static java.security.KeyPair keys() {
        try {
            var g = java.security.KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeAll
    static void start() {
        infra = MessagingInfrastructureFixture.start();
        for (String schema : List.of("planner", "activity"))
            Flyway.configure()
                .dataSource(infra.jdbcUrl(schema), "postgres", "postgres")
                .schemas(schema)
                .defaultSchema(schema)
                .baselineOnMigrate(true)
                .baselineVersion("2")
                .locations(
                    "filesystem:" +
                        Path.of(
                            System.getProperty("todorok.repository.root"),
                            "services",
                            schema + "-service",
                            "src/main/resources/db/migration"
                        )
                            .toString()
                            .replace('\\', '/')
                )
                .load()
                .migrate();
        activity = app(ActivityApplication.class, "activity");
        planner = app(PlannerApplication.class, "planner");
    }

    static ConfigurableApplicationContext app(Class<?> main, String schema) {
        return new SpringApplicationBuilder(main).run(
            "--server.port=0",
            "--server.servlet.context-path=/api/" + schema + "/v1",
            "--todorok.auth.allowed-origins=https://todorok.test",
            "--todorok.template-service.public-key=" + (schema.equals("activity") ? Base64.getEncoder().encodeToString(SERVICE_KEYS.getPublic().getEncoded()) : ""),
            "--todorok.template-service.private-key=" + (schema.equals("planner") ? Base64.getEncoder().encodeToString(SERVICE_KEYS.getPrivate().getEncoded()) : ""),
            "--todorok.template-service.base-url=http://localhost:" + (activity == null ? 1 : activity.getEnvironment().getProperty("local.server.port")) + "/api/activity/v1",
            "--spring.datasource.url=" + infra.jdbcUrl(schema),
            "--spring.datasource.username=postgres",
            "--spring.datasource.password=postgres",
            "--spring.flyway.enabled=false",
            "--spring.jpa.properties.hibernate.default_schema=" + schema,
            "--spring.jpa.hibernate.ddl-auto=validate",
            "--spring.kafka.bootstrap-servers=" + infra.bootstrapServers(),
            "--todorok.messaging.enabled=true",
            "--spring.kafka.consumer.auto-offset-reset=earliest",
            "--spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
            "--spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
            "--spring.jackson.deserialization.fail-on-unknown-properties=true",
            "--spring.jackson.deserialization.accept-float-as-int=false",
            "--spring.jackson.mapper.allow-coercion-of-scalars=false",
            "--todorok.auth.public-key=" +
                Base64.getEncoder().encodeToString(
                    KEYS.getPublic().getEncoded()
                ),
            "--todorok.auth.private-key=" +
                Base64.getEncoder().encodeToString(
                    KEYS.getPrivate().getEncoded()
                )
        );
    }

    @AfterAll
    static void stop() {
        if (activity != null) activity.close();
        if (planner != null) planner.close();
        if (infra != null) infra.close();
    }

    static HttpResponse<String> call(
        String service,
        UUID owner,
        String method,
        String path,
        String body
    ) throws Exception {
        var ctx = service.equals("activity") ? activity : planner;
        var jwt = new SignedJWT(
            new JWSHeader(JWSAlgorithm.RS256),
            new JWTClaimsSet.Builder()
                .issuer("todorok")
                .audience("todorok-api")
                .subject(owner.toString())
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .build()
        );
        jwt.sign(new RSASSASigner(KEYS.getPrivate()));
        var request = HttpRequest.newBuilder(
            URI.create(
                "http://localhost:" +
                    ctx.getEnvironment().getProperty("local.server.port") +
                    "/api/" +
                    service +
                    "/v1" +
                    path
            )
        )
            .header("Authorization", "Bearer " + jwt.serialize())
            .header("Content-Type", "application/json")
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body)
            )
            .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    static JsonNode ok(HttpResponse<String> response, int status) {
        assertThat(response.statusCode())
            .withFailMessage(response.body())
            .isEqualTo(status);
        return JSON.readTree(response.body());
    }

    static String task(UUID owner, String type) throws Exception {
        String id = ok(
            call(
                "planner",
                owner,
                "POST",
                "/tasks",
                JSON.writeValueAsString(
                    Map.of(
                        "title",
                        "기록",
                        "taskType",
                        type,
                        "scheduledDate",
                        "2026-09-07"
                    )
                )
            ),
            201
        )
            .get("taskId")
            .asText();
        await(
            () ->
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.task_reference where task_id=?",
                        Integer.class,
                        UUID.fromString(id)
                    ) == 1
        );
        return id;
    }

    static String request(
        String task,
        String type,
        String status,
        UUID command,
        String detail
    ) {
        return (
            "{\"taskId\":\"" +
            task +
            "\",\"commandId\":\"" +
            command +
            "\",\"activityType\":\"" +
            type +
            "\",\"completionStatus\":\"" +
            status +
            "\",\"performedAt\":\"2026-09-08T01:00:00+09:00\",\"note\":\"완료 메모\",\"detail\":" +
            detail +
            "}"
        );
    }

    static void await(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("Service synchronization timed out");
    }

    static void applied(UUID owner, String id) {
        await(() -> {
            try {
                return JSON.readTree(
                    call(
                        "activity",
                        owner,
                        "GET",
                        "/activities/" + id,
                        null
                    ).body()
                )
                    .path("syncState")
                    .asText()
                    .equals("APPLIED");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    static void send(String topic, String key, String json) throws Exception {
        try (
            var producer = new KafkaProducer<String, String>(
                Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    infra.bootstrapServers(),
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class
                )
            )
        ) {
            producer
                .send(new ProducerRecord<>(topic, key, json))
                .get(15, TimeUnit.SECONDS);
        }
    }

    @Test
    void typedRoundTripReplaysAndActualInterval() throws Exception {
        for (var entry : Map.of(
            "WORKOUT",
            "{\"workout\":{\"sets\":[{\"exercise\":\"스쿼트\",\"reps\":5,\"weightKg\":60}]}}",
            "STUDY",
            "{\"study\":{\"subject\":\"Java\",\"durationMinutes\":30,\"values\":{\"pages\":10},\"snapshot\":{\"label\":\"독서\"}}}",
            "CLIMBING",
            "{\"climbing\":{\"durationSeconds\":1800,\"rounds\":[{\"grade\":\"V3\",\"attempts\":2,\"completed\":true}]}}"
        ).entrySet()) {
            UUID owner = UUID.randomUUID();
            String task = task(owner, entry.getKey());
            UUID command = UUID.randomUUID();
            String body = request(
                task,
                entry.getKey(),
                "COMPLETED",
                command,
                entry.getValue()
            );
            body =
                body.substring(0, body.length() - 1) +
                ",\"startedAt\":\"2026-09-08T01:00:00+09:00\",\"endedAt\":\"2026-09-08T01:30:00+09:00\"}";
            var saved = ok(
                call("activity", owner, "POST", "/activities", body),
                201
            );
            String id = saved.get("activityId").asText();
            applied(owner, id);
            var done = ok(
                call("planner", owner, "GET", "/tasks/" + task, null),
                200
            );
            assertThat(done.get("status").asText()).isEqualTo("COMPLETED");
            assertThat(done.get("scheduledDate").asText()).isEqualTo(
                "2026-09-08"
            );
            assertThat(done.get("activityId").asText()).isEqualTo(id);
            assertThat(done.get("startedAt").isNull()).isFalse();
            assertThat(
                ok(
                    call("activity", owner, "GET", "/activities/" + id, null),
                    200
                )
                    .get("note")
                    .asText()
            ).isEqualTo("완료 메모");
            var persisted = ok(
                call("activity", owner, "GET", "/activities/" + id, null),
                200
            ).get("detail");
            switch (entry.getKey()) {
                case "WORKOUT" -> {
                    var set = persisted.get("workout").get("sets").get(0);
                    assertThat(set.get("exercise").asText()).isEqualTo(
                        "스쿼트"
                    );
                    assertThat(set.get("reps").asInt()).isEqualTo(5);
                    assertThat(
                        set.get("weightKg").decimalValue()
                    ).isEqualByComparingTo("60");
                }
                case "STUDY" -> {
                    var study = persisted.get("study");
                    assertThat(study.get("durationMinutes").asInt()).isEqualTo(
                        30
                    );
                    assertThat(
                        study.get("values").get("pages").asInt()
                    ).isEqualTo(10);
                    assertThat(
                        study.get("snapshot").get("label").asText()
                    ).isEqualTo("독서");
                }
                case "CLIMBING" -> {
                    var climbing = persisted.get("climbing");
                    assertThat(
                        climbing.get("durationSeconds").asInt()
                    ).isEqualTo(1800);
                    assertThat(
                        climbing.get("rounds").get(0).get("attempts").asInt()
                    ).isEqualTo(2);
                    assertThat(
                        climbing
                            .get("rounds")
                            .get(0)
                            .get("completed")
                            .asBoolean()
                    ).isTrue();
                }
                default -> throw new AssertionError("Unknown test type");
            }
            assertThat(
                ok(call("activity", owner, "POST", "/activities", body), 201)
                    .get("activityId")
                    .asText()
            ).isEqualTo(id);
            ok(
                call(
                    "activity",
                    owner,
                    "POST",
                    "/activities",
                    body.replace("완료 메모", "변경")
                ),
                409
            );
            ok(
                call(
                    "activity",
                    UUID.randomUUID(),
                    "GET",
                    "/activities/" + id,
                    null
                ),
                404
            );
            ok(
                call(
                    "planner",
                    owner,
                    "DELETE",
                    "/tasks/" +
                        task +
                        "?version=" +
                        done.get("version").asLong(),
                    null
                ),
                204
            );
            await(() ->
                infra
                    .database()
                    .queryForObject(
                        "select status from activity.task_reference where task_id=?",
                        String.class,
                        UUID.fromString(task)
                    )
                    .equals("DELETED")
            );
            assertThat(
                ok(call("activity", owner, "POST", "/activities", body), 201)
                    .get("activityId")
                    .asText()
            ).isEqualTo(id);
        }
    }

    @Test
    void concurrentCommandsPartialAndAtomicRollback() throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "WORKOUT");
        String body = request(
            task,
            "WORKOUT",
            "COMPLETED",
            UUID.randomUUID(),
            "{}"
        );
        var container = planner
            .getBean(KafkaListenerEndpointRegistry.class)
            .getListenerContainer("planner-activity");
        container.stop();
        try {
            var start = new CountDownLatch(1);
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = new ArrayList<Future<HttpResponse<String>>>();
                for (int i = 0; i < 8; i++) futures.add(
                    pool.submit(() -> {
                        start.await();
                        return call(
                            "activity",
                            owner,
                            "POST",
                            "/activities",
                            body
                        );
                    })
                );
                start.countDown();
                var ids = new HashSet<String>();
                for (var future : futures)
                    ids.add(ok(future.get(), 201).get("activityId").asText());
                assertThat(ids).hasSize(1);
            }
            ok(
                call(
                    "activity",
                    owner,
                    "POST",
                    "/activities",
                    request(
                        task,
                        "WORKOUT",
                        "COMPLETED",
                        UUID.randomUUID(),
                        "{}"
                    )
                ),
                409
            );
            String partialTask = task(owner, "STUDY");
            var partial = ok(
                call(
                    "activity",
                    owner,
                    "POST",
                    "/activities",
                    request(
                        partialTask,
                        "STUDY",
                        "PARTIAL",
                        UUID.randomUUID(),
                        "{}"
                    )
                ),
                201
            );
            assertThat(partial.get("syncState").asText()).isEqualTo(
                "NOT_REQUIRED"
            );
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.outbox_event where aggregateid=?",
                        Integer.class,
                        partial.get("activityId").asText()
                    )
            ).isZero();
            assertThat(
                ok(
                    call(
                        "planner",
                        owner,
                        "GET",
                        "/tasks/" + partialTask,
                        null
                    ),
                    200
                )
                    .get("status")
                    .asText()
            ).isEqualTo("PLANNED");
            String rollbackTask = task(owner, "CLIMBING");
            infra
                .database()
                .execute(
                    "alter table activity.outbox_event add constraint fail_test check(payload->>'userId'<>'" +
                        owner +
                        "') not valid"
                );
            try {
                ok(
                    call(
                        "activity",
                        owner,
                        "POST",
                        "/activities",
                        request(
                            rollbackTask,
                            "CLIMBING",
                            "COMPLETED",
                            UUID.randomUUID(),
                            "{\"climbing\":{\"rounds\":[{\"attempts\":1}]}}"
                        )
                    ),
                    500
                );
            } finally {
                infra
                    .database()
                    .execute(
                        "alter table activity.outbox_event drop constraint fail_test"
                    );
            }
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.activity_record where task_id=?",
                        Integer.class,
                        UUID.fromString(rollbackTask)
                    )
            ).isZero();
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.climbing_round r join activity.activity_record a on a.id=r.activity_id where a.user_id=?",
                        Integer.class,
                        owner
                    )
            ).isZero();
        } finally {
            container.start();
        }
    }

    @Test
    void stoppedConsumerRestartDeletionAndSkipConflictsRemainReadable()
        throws Exception {
        for (String command : List.of("delete", "skip")) {
            UUID owner = UUID.randomUUID();
            String task = task(owner, "STUDY");
            var listener = planner
                .getBean(KafkaListenerEndpointRegistry.class)
                .getListenerContainer("planner-activity");
            listener.stop();
            String id;
            try {
                var saved = ok(
                    call(
                        "activity",
                        owner,
                        "POST",
                        "/activities",
                        request(
                            task,
                            "STUDY",
                            "COMPLETED",
                            UUID.randomUUID(),
                            "{}"
                        )
                    ),
                    201
                );
                id = saved.get("activityId").asText();
                assertThat(saved.get("syncState").asText()).isEqualTo(
                    "PENDING"
                );
                if (command.equals("delete")) ok(
                    call(
                        "planner",
                        owner,
                        "DELETE",
                        "/tasks/" + task + "?version=0",
                        null
                    ),
                    204
                );
                else ok(
                    call(
                        "planner",
                        owner,
                        "POST",
                        "/tasks/" + task + "/skip",
                        "{\"version\":0}"
                    ),
                    200
                );
                assertThat(
                    ok(
                        call(
                            "activity",
                            owner,
                            "GET",
                            "/activities/" + id,
                            null
                        ),
                        200
                    )
                        .get("syncState")
                        .asText()
                ).isEqualTo("PENDING");
            } finally {
                listener.start();
            }
            await(() ->
                infra
                    .database()
                    .queryForObject(
                        "select sync_state from activity.activity_record where id=?",
                        String.class,
                        UUID.fromString(id)
                    )
                    .equals("CONFLICT")
            );
            var conflict = ok(
                call("activity", owner, "GET", "/activities/" + id, null),
                200
            );
            assertThat(conflict.get("status").asText()).isEqualTo("COMPLETED");
            assertThat(conflict.get("syncReason").asText()).isEqualTo(
                command.equals("delete") ? "TASK_DELETED" : "TASK_SKIPPED"
            );
        }
    }

    @Test
    void duplicateAndLowerVersionKafkaDeliveryCannotResurrectVoidedCompletionOrDeletedReference()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "WORKOUT");
        var saved = ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                request(task, "WORKOUT", "COMPLETED", UUID.randomUUID(), "{}")
            ),
            201
        );
        String id = saved.get("activityId").asText();
        applied(owner, id);
        String original = infra
            .database()
            .queryForObject(
                "select payload::text from activity.outbox_event where aggregateid=? and type='ACTIVITY_COMPLETED'",
                String.class,
                id
            );
        send("todorok.activity.v1", id, original);
        var replay = (tools.jackson.databind.node.ObjectNode) JSON.readTree(
            original
        );
        replay.put("eventId", UUID.randomUUID().toString());
        send("todorok.activity.v1", id, replay.toString());
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities/" + id + "/void",
                "{\"reason\":\"취소\",\"version\":0}"
            ),
            200
        );
        applied(owner, id);
        replay.put("eventId", UUID.randomUUID().toString());
        String last = replay.get("eventId").asText();
        send("todorok.activity.v1", id, replay.toString());
        await(
            () ->
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from planner.processed_event where event_id=?",
                        Integer.class,
                        UUID.fromString(last)
                    ) == 1
        );
        assertThat(
            ok(call("planner", owner, "GET", "/tasks/" + task, null), 200)
                .get("status")
                .asText()
        ).isEqualTo("PLANNED");
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select revision from planner.activity_completion_result where activity_id=?",
                    Long.class,
                    UUID.fromString(id)
                )
        ).isEqualTo(1);
    }

    @Test
    void validatesOwnersTypesUnknownFieldsAndActualIntervalsBeforeWriting()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "WORKOUT");
        String base = request(
            task,
            "WORKOUT",
            "PARTIAL",
            UUID.randomUUID(),
            "{}"
        );
        ok(
            call("activity", UUID.randomUUID(), "POST", "/activities", base),
            404
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.replace(task, UUID.randomUUID().toString())
            ),
            409
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.replace("WORKOUT", "STUDY")
            ),
            409
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.replace("\"detail\":{}", "\"detail\":{\"study\":{}}")
            ),
            400
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.replace("\"detail\":{}", "\"detail\":{\"surprise\":1}")
            ),
            400
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.replace(
                    "\"detail\":{}",
                    "\"detail\":{\"workout\":{\"sets\":[{\"reps\":\"5\"}]}}"
                )
            ),
            400
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.substring(0, base.length() - 1) +
                    ",\"startedAt\":\"2026-09-08T01:00:00+09:00\"}"
            ),
            400
        );
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from activity.activity_record where task_id=?",
                    Integer.class,
                    UUID.fromString(task)
                )
        ).isZero();
    }

    @Test
    void competingCommandsHaveOneWinnerAndSeriesAdvancesOnlyForCompletion()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String series = ok(
            call(
                "planner",
                owner,
                "POST",
                "/series",
                "{\"title\":\"반복\",\"taskType\":\"STUDY\",\"startDate\":\"2026-09-07\",\"rule\":{\"frequency\":\"DAILY\",\"interval\":1,\"weekdays\":[],\"monthDay\":1}}"
            ),
            201
        )
            .get("seriesId")
            .asText();
        String task = infra
            .database()
            .queryForObject(
                "select id::text from planner.task where series_id=?",
                String.class,
                UUID.fromString(series)
            );
        await(
            () ->
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.task_reference where task_id=?",
                        Integer.class,
                        UUID.fromString(task)
                    ) == 1
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                request(task, "STUDY", "PARTIAL", UUID.randomUUID(), "{}")
            ),
            201
        );
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from planner.task where series_id=?",
                    Integer.class,
                    UUID.fromString(series)
                )
        ).isOne();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new CountDownLatch(1);
            var futures = new ArrayList<Future<HttpResponse<String>>>();
            for (int i = 0; i < 6; i++) futures.add(
                pool.submit(() -> {
                    start.await();
                    return call(
                        "activity",
                        owner,
                        "POST",
                        "/activities",
                        request(
                            task,
                            "STUDY",
                            "COMPLETED",
                            UUID.randomUUID(),
                            "{}"
                        )
                    );
                })
            );
            start.countDown();
            var statuses = new ArrayList<Integer>();
            String id = null;
            for (var future : futures) {
                var response = future.get();
                statuses.add(response.statusCode());
                if (response.statusCode() == 201) id = JSON.readTree(
                    response.body()
                )
                    .get("activityId")
                    .asText();
            }
            assertThat(statuses).containsOnly(201, 409);
            assertThat(statuses.stream().filter(s -> s == 201)).hasSize(1);
            applied(owner, id);
        }
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from planner.task where series_id=?",
                    Integer.class,
                    UUID.fromString(series)
                )
        ).isEqualTo(2);
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from planner.task where series_id=? and status='PLANNED'",
                    Integer.class,
                    UUID.fromString(series)
                )
        ).isOne();
    }

    @Test
    void inboxCompletionAndAckRollbackTogetherThenKafkaRestartRecovers()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "STUDY");
        var listener = planner
            .getBean(KafkaListenerEndpointRegistry.class)
            .getListenerContainer("planner-activity");
        listener.stop();
        String id;
        try {
            id = ok(
                call(
                    "activity",
                    owner,
                    "POST",
                    "/activities",
                    request(task, "STUDY", "COMPLETED", UUID.randomUUID(), "{}")
                ),
                201
            )
                .get("activityId")
                .asText();
            String event = infra
                .database()
                .queryForObject(
                    "select payload::text from activity.outbox_event where aggregateid=?",
                    String.class,
                    id
                );
            UUID eventId = UUID.fromString(
                JSON.readTree(event).get("eventId").asText()
            );
            infra
                .database()
                .execute(
                    "alter table planner.outbox_event add constraint fail_ack check(type<>'ACTIVITY_SYNC_RESULT') not valid"
                );
            try {
                assertThatThrownBy(() ->
                    planner
                        .getBean(
                            io.todorok.planner.task.ActivityCompletionConsumer.class
                        )
                        .receive(event)
                ).isInstanceOf(RuntimeException.class);
            } finally {
                infra
                    .database()
                    .execute(
                        "alter table planner.outbox_event drop constraint fail_ack"
                    );
            }
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from planner.processed_event where event_id=?",
                        Integer.class,
                        eventId
                    )
            ).isZero();
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from planner.activity_completion_result where activity_id=?",
                        Integer.class,
                        UUID.fromString(id)
                    )
            ).isZero();
            assertThat(
                ok(call("planner", owner, "GET", "/tasks/" + task, null), 200)
                    .get("status")
                    .asText()
            ).isEqualTo("PLANNED");
        } finally {
            listener.start();
        }
        applied(owner, id);
    }

    @Test
    void deletedReferenceRejectsLowerAndHigherResurrectionAndOwnerCorruptionRollsBackInbox()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "WORKOUT");
        String original = infra
            .database()
            .queryForObject(
                "select payload::text from planner.outbox_event where aggregateid=? and type='TASK_SCHEDULED'",
                String.class,
                task
            );
        ok(
            call(
                "planner",
                owner,
                "DELETE",
                "/tasks/" + task + "?version=0",
                null
            ),
            204
        );
        await(() ->
            infra
                .database()
                .queryForObject(
                    "select status from activity.task_reference where task_id=?",
                    String.class,
                    UUID.fromString(task)
                )
                .equals("DELETED")
        );
        for (long version : List.of(0L, 99L)) {
            var replay = (tools.jackson.databind.node.ObjectNode) JSON.readTree(
                original
            );
            UUID eventId = UUID.randomUUID();
            replay.put("eventId", eventId.toString());
            replay.put("aggregateVersion", version);
            send("todorok.task.v1", task, replay.toString());
            await(
                () ->
                    infra
                        .database()
                        .queryForObject(
                            "select count(*) from activity.processed_event where event_id=?",
                            Integer.class,
                            eventId
                        ) == 1
            );
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select status from activity.task_reference where task_id=?",
                        String.class,
                        UUID.fromString(task)
                    )
            ).isEqualTo("DELETED");
        }
        var corrupt = (tools.jackson.databind.node.ObjectNode) JSON.readTree(
            original
        );
        UUID eventId = UUID.randomUUID();
        corrupt.put("eventId", eventId.toString());
        corrupt.put("userId", UUID.randomUUID().toString());
        assertThatThrownBy(() ->
            activity
                .getBean(io.todorok.activity.record.ActivityEventConsumer.class)
                .receive(corrupt.toString())
        ).isInstanceOf(IllegalArgumentException.class);
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from activity.processed_event where event_id=?",
                    Integer.class,
                    eventId
                )
        ).isZero();
    }
}
