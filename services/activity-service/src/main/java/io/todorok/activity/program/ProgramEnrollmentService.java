package io.todorok.activity.program;

import io.todorok.web.ApiFailure;
import io.todorok.contracts.EventEnvelope;
import io.todorok.contracts.EventType;
import io.todorok.messaging.OutboxEventWriter;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProgramEnrollmentService {
    public record Enrollment(UUID id, String catalogKey, long catalogVersion, int recommendedWeek, int startWeek, int currentWeek, int currentSession, String status, UUID sessionId, List<Integer> targetSets) {}
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ProgramCatalogImporter importer;
    private final OutboxEventWriter outbox;
    private final Clock clock;
    public ProgramEnrollmentService(JdbcTemplate jdbc, ObjectMapper mapper, OutboxEventWriter outbox, Clock clock) { this.jdbc = jdbc; this.mapper = mapper; this.importer = new ProgramCatalogImporter(mapper); this.outbox = outbox; this.clock = clock; }

    @Transactional
    public Enrollment enroll(UUID owner, UUID commandId, String key, long version, int initialTestValue, Integer startWeekOverride) {
        if (initialTestValue < 0) throw invalid("INITIAL_TEST_INVALID", "Initial test value must be non-negative.");
        var existing = jdbc.queryForList("select id,request_fingerprint from program_enrollment where user_id=? and command_id=? for update", owner, commandId);
        String fingerprint = fingerprint(key, version, initialTestValue, startWeekOverride);
        if (!existing.isEmpty()) {
            if (!fingerprint.equals(existing.getFirst().get("request_fingerprint"))) throw invalid("COMMAND_CONFLICT", "A command ID cannot change enrollment input.");
            return read((UUID) existing.getFirst().get("id"));
        }
        var rows = jdbc.queryForList("select definition::text as definition from program_catalog where catalog_key=? and catalog_version=? for update", key, version);
        if (rows.isEmpty()) throw new ApiFailure(404, "PROGRAM_NOT_FOUND", "Program not found", "Catalog key and version were not found.", false);
        var catalog = importer.parse(mapper.readTree(String.valueOf(rows.getFirst().get("definition"))));
        int recommended = 1;
        int start = startWeekOverride == null ? recommended : startWeekOverride;
        if (start < 1 || start > catalog.weeks().size()) throw invalid("START_WEEK_INVALID", "Start week is outside this catalog.");
        var first = catalog.weeks().get(start - 1).sessions().getFirst();
        UUID id = UUID.randomUUID(), sessionId = UUID.randomUUID(), taskId = UUID.randomUUID();
        jdbc.update("insert into program_enrollment(id,user_id,catalog_key,catalog_version,command_id,request_fingerprint,initial_test_value,recommended_week,start_week,current_week,current_session,status) values (?,?,?,?,?,?,?,?,?,?,?,'ACTIVE')",
            id, owner, key, version, commandId, fingerprint, initialTestValue, recommended, start, start, first.session());
        jdbc.update("insert into program_session(id,enrollment_id,cycle,week,session,target_sets,task_id) values (?,?,1,?,?,cast(? as jsonb),?)",
            sessionId, id, start, first.session(), mapper.writeValueAsString(first.sets()), taskId);
        var date = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        outbox.append("program-session", sessionId.toString(), new EventEnvelope<>(UUID.randomUUID(), EventType.PROGRAM_SESSION_REQUESTED, 1, 0, clock.instant(), owner,
            Map.of("enrollmentId", id.toString(), "sessionId", sessionId.toString(), "taskId", taskId.toString(), "title", catalog.name() + " · " + start + "주차 " + first.session() + "회", "scheduledDate", date.toString(), "targetSets", first.sets())));
        return new Enrollment(id, key, version, recommended, start, start, first.session(), "ACTIVE", sessionId, first.sets());
    }

    @Transactional(readOnly = true)
    public Enrollment get(UUID owner, UUID id) {
        var rows = jdbc.queryForList("select id from program_enrollment where id=? and user_id=?", id, owner);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Not found", "Program enrollment was not found.", false);
        return read(id);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> list(UUID owner) {
        return jdbc.queryForList("select id from program_enrollment where user_id=? order by created_at desc,id desc", owner).stream()
            .map(row -> read((UUID) row.get("id"))).toList();
    }

    @Transactional
    public void recordOutcome(UUID taskId, UUID activityId, int actualRepetitions, boolean voided) {
        var sessions = jdbc.queryForList("select * from program_session where task_id=? for update", taskId);
        if (sessions.isEmpty()) return;
        var session = sessions.getFirst();
        UUID enrollmentId = (UUID) session.get("enrollment_id");
        var enrollment = jdbc.queryForMap("select * from program_enrollment where id=? for update", enrollmentId);
        int target = 0;
        for (var value : mapper.readTree(String.valueOf(jdbc.queryForObject("select target_sets::text from program_session where id=?", String.class, session.get("id"))))) target += value.asInt();
        String outcome = voided ? "VOIDED" : actualRepetitions >= target ? "SUCCESS" : "FAILURE";
        jdbc.update("update program_session set activity_id=?,outcome=? where id=?", voided ? null : activityId, outcome, session.get("id"));
        var catalog = importer.parse(mapper.readTree(String.valueOf(jdbc.queryForObject("select definition::text from program_catalog where catalog_key=? and catalog_version=?", String.class, enrollment.get("catalog_key"), enrollment.get("catalog_version")))));
        var history = jdbc.query("select row_number() over(order by created_at,id) as sequence,outcome from program_session where enrollment_id=? and outcome is not null", (row, index) -> new ProgramProgressPolicy.Attempt(row.getLong("sequence"), ProgramProgressPolicy.Outcome.valueOf(row.getString("outcome"))), enrollmentId);
        var progress = new ProgramProgressPolicy().calculate(((Number) enrollment.get("start_week")).intValue(), catalog.weeks().size(), catalog.sessionsPerWeek(), history);
        int currentCycle = ((Number) enrollment.get("current_cycle")).intValue();
        int nextCycle = progress.session() == 1 && !progress.completed() && history.size() % catalog.sessionsPerWeek() == 0 ? currentCycle + 1 : currentCycle;
        jdbc.update("update program_enrollment set current_cycle=?,current_week=?,current_session=?,status=?,revision=revision+1 where id=?", nextCycle, progress.week(), progress.session(), progress.completed() ? "COMPLETED" : "ACTIVE", enrollmentId);
        if (progress.completed()) return;
        var existing = jdbc.queryForList("select id from program_session where enrollment_id=? and cycle=? and session=?", enrollmentId, nextCycle, progress.session());
        if (!existing.isEmpty()) return;
        var nextTarget = catalog.weeks().get(progress.week() - 1).sessions().get(progress.session() - 1);
        UUID nextSession = UUID.randomUUID(), nextTask = UUID.randomUUID();
        jdbc.update("insert into program_session(id,enrollment_id,cycle,week,session,target_sets,task_id) values (?,?,?,?,?,cast(? as jsonb),?)", nextSession, enrollmentId, nextCycle, progress.week(), progress.session(), mapper.writeValueAsString(nextTarget.sets()), nextTask);
        UUID owner = (UUID) enrollment.get("user_id");
        LocalDate date = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))).plusDays(1);
        outbox.append("program-session", nextSession.toString(), new EventEnvelope<>(UUID.randomUUID(), EventType.PROGRAM_SESSION_REQUESTED, 1, ((Number) enrollment.get("revision")).longValue() + 1, clock.instant(), owner,
            Map.of("enrollmentId", enrollmentId.toString(), "sessionId", nextSession.toString(), "taskId", nextTask.toString(), "title", catalog.name() + " · " + progress.week() + "주차 " + progress.session() + "회", "scheduledDate", date.toString(), "targetSets", nextTarget.sets())));
    }

    private Enrollment read(UUID id) {
        var enrollment = jdbc.queryForMap("select * from program_enrollment where id=?", id);
        var session = jdbc.queryForMap("select id,target_sets::text as target_sets from program_session where enrollment_id=? and cycle=? and session=?", id, enrollment.get("current_cycle"), enrollment.get("current_session"));
        var sets = mapper.readTree(String.valueOf(session.get("target_sets")));
        var targets = new ArrayList<Integer>(); sets.forEach(value -> targets.add(value.asInt()));
        return new Enrollment(id, (String) enrollment.get("catalog_key"), ((Number) enrollment.get("catalog_version")).longValue(), ((Number) enrollment.get("recommended_week")).intValue(), ((Number) enrollment.get("start_week")).intValue(), ((Number) enrollment.get("current_week")).intValue(), ((Number) enrollment.get("current_session")).intValue(), (String) enrollment.get("status"), (UUID) session.get("id"), List.copyOf(targets));
    }
    private String fingerprint(String key, long version, int initial, Integer override) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((key + "|" + version + "|" + initial + "|" + Objects.toString(override, "-")).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private ApiFailure invalid(String code, String detail) { return new ApiFailure(400, code, "Invalid program enrollment", detail, false); }
}
