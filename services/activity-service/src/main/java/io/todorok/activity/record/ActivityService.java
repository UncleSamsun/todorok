package io.todorok.activity.record;

import io.todorok.activity.api.model.*;
import io.todorok.contracts.*;
import io.todorok.contracts.events.*;
import io.todorok.messaging.OutboxEventWriter;
import io.todorok.activity.program.ProgramEnrollmentService;
import io.todorok.web.ApiFailure;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class ActivityService {

    // All correlated detail reads belong to the same PostgreSQL statement snapshot as the header.
    private static final String SNAPSHOT_SELECT = """
        select a.*, case a.activity_type
          when 'WORKOUT' then jsonb_build_object('workout', jsonb_build_object('sets',
            coalesce((select jsonb_agg(jsonb_build_object('exercise',s.exercise,'reps',s.reps,
              'weightKg',s.weight_kg,'durationSeconds',s.duration_seconds) order by s.position)
              from workout_set s where s.activity_id=a.id), '[]'::jsonb), 'fields',
            coalesce((select jsonb_agg(jsonb_strip_nulls(jsonb_build_object('fieldId',v.field_id,'type',v.type,
              'numberValue',case when v.type='NUMBER' then v.number_value end,
              'timeSeconds',case when v.type='TIME' then v.time_seconds end,
              'textValue',case when v.type='SHORT_TEXT' then v.text_value end,
              'checked',case when v.type='CHECK' then v.checked end,
              'memoValue',case when v.type='MEMO' then v.memo_value end)) order by d.position)
              from activity_field_value v join template_field_definition d
                on d.template_id=v.template_id and d.version=v.template_version and d.field_id=v.field_id
              where v.activity_id=a.id), '[]'::jsonb)))
          when 'STUDY' then jsonb_build_object('study',
            (select jsonb_build_object('subject',s.subject,'durationMinutes',s.duration_minutes,
              'values',s.values_json,'snapshot',s.snapshot) from study_detail s where s.activity_id=a.id))
          when 'CLIMBING' then jsonb_build_object('climbing', jsonb_build_object(
            'durationSeconds',(select c.duration_seconds from climbing_detail c where c.activity_id=a.id),
            'rounds',coalesce((select jsonb_agg(jsonb_build_object('grade',c.grade,'attempts',c.attempts,
              'completed',c.completed) order by c.position) from climbing_round c where c.activity_id=a.id), '[]'::jsonb),
            'fields',coalesce((select jsonb_agg(jsonb_strip_nulls(jsonb_build_object('fieldId',v.field_id,'type',v.type,
              'numberValue',case when v.type='NUMBER' then v.number_value end,
              'timeSeconds',case when v.type='TIME' then v.time_seconds end,
              'textValue',case when v.type='SHORT_TEXT' then v.text_value end,
              'checked',case when v.type='CHECK' then v.checked end,
              'memoValue',case when v.type='MEMO' then v.memo_value end)) order by d.position)
              from activity_field_value v join template_field_definition d
                on d.template_id=v.template_id and d.version=v.template_version and d.field_id=v.field_id
              where v.activity_id=a.id), '[]'::jsonb)))
        end as detail_snapshot from activity_record a
        """;

    private final JdbcTemplate jdbc;
    private final ActivityDetailStore details;
    private final OutboxEventWriter outbox;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final ActivityTemplateRecords templates;
    private final ProgramEnrollmentService programs;

    public ActivityService(
        JdbcTemplate jdbc,
        ActivityDetailStore details,
        OutboxEventWriter outbox,
        ObjectMapper mapper,
        Clock clock
    ) {
        this(jdbc, details, outbox, mapper, clock, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ActivityService(
        JdbcTemplate jdbc,
        ActivityDetailStore details,
        OutboxEventWriter outbox,
        ObjectMapper mapper,
        Clock clock,
        ProgramEnrollmentService programs
    ) {
        this.jdbc = jdbc;
        this.details = details;
        this.outbox = outbox;
        this.mapper = mapper;
        this.clock = clock;
        this.templates = new ActivityTemplateRecords(jdbc);
        this.programs = programs;
    }

    @Transactional
    public ActivityResponse create(UUID owner, CreateActivityRequest request) {
        details.validate(request.getActivityType(), request.getDetail());
        var start = request.getStartedAt();
        var end = request.getEndedAt();
        var zone = ZoneId.of("Asia/Seoul");
        var date = request
            .getPerformedAt()
            .atZoneSameInstant(zone)
            .toLocalDate();
        if (
            (start == null) != (end == null) ||
            (start != null &&
                (!end.isAfter(start) ||
                    !start.atZoneSameInstant(zone).toLocalDate().equals(date) ||
                    !end.atZoneSameInstant(zone).toLocalDate().equals(date)))
        ) throw new ApiFailure(
            400,
            "INVALID_INTERVAL",
            "Invalid interval",
            "Use an ordered start/end pair on the performed Seoul date.",
            false
        );
        String fingerprint = fingerprint(request);
        // Serializes an owner/command even before its row exists. Hash collisions only serialize unrelated commands.
        jdbc.queryForList(
            "select pg_advisory_xact_lock(hashtextextended(?,0))",
            owner + ":" + request.getCommandId()
        );
        var previous = jdbc.queryForList(
            "select id,fingerprint from activity_record where user_id=? and command_id=?",
            owner,
            request.getCommandId()
        );
        if (!previous.isEmpty()) {
            if (
                !fingerprint.equals(previous.getFirst().get("fingerprint"))
            ) throw fail("COMMAND_CONFLICT", false);
            return get(owner, (UUID) previous.getFirst().get("id"));
        }
        var references = jdbc.queryForList(
            "select * from task_reference where task_id=? for update",
            request.getTaskId()
        );
        if (references.isEmpty()) throw fail("TASK_REFERENCE_PENDING", true);
        var reference = references.getFirst();
        if (!owner.equals(reference.get("user_id"))) throw new ApiFailure(
            404,
            "NOT_FOUND",
            "Not found",
            "Task was not found.",
            false
        );
        if (
            !request.getActivityType().name().equals(reference.get("task_type"))
        ) throw fail("TASK_TYPE_MISMATCH", false);
        var template = templates.current(owner, reference, request.getExpectedTemplateVersion());
        var templateValues = templates.validate(request.getDetail(), template);
        if (!"PLANNED".equals(reference.get("status"))) throw fail(
            "TASK_" + reference.get("status"),
            false
        );
        if (
            request.getCompletionStatus() ==
                ActivityCompletionStatus.COMPLETED &&
            !jdbc
                .queryForList(
                    "select id from activity_record where task_id=? and status='COMPLETED'",
                    request.getTaskId()
                )
                .isEmpty()
        ) throw fail("TASK_ALREADY_RECORDED", false);
        UUID id = UUID.randomUUID();
        boolean complete =
            request.getCompletionStatus() == ActivityCompletionStatus.COMPLETED;
        jdbc.update(
            """
            insert into activity_record(id,user_id,task_id,activity_type,performed_at,status,command_id,fingerprint,note,sync_state,started_at,ended_at,
              detail_format,template_id,template_version,template_binding_id,template_snapshot)
            values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,cast(? as jsonb))
            """,
            id,
            owner,
            request.getTaskId(),
            request.getActivityType().name(),
            request.getPerformedAt(),
            request.getCompletionStatus().name(),
            request.getCommandId(),
            fingerprint,
            request.getNote(),
            complete ? "PENDING" : "NOT_REQUIRED",
            start,
            end,
            template == null ? "STANDARD" : "TEMPLATE",
            template == null ? null : template.getTemplateId(),
            template == null ? null : template.getTemplateVersion(),
            template == null ? null : reference.get("template_binding_id"),
            template == null ? null : mapper.writeValueAsString(template)
        );
        details.save(id, request.getActivityType(), request.getDetail(), template, template == null ? null : templateValues);
        if (complete && request.getActivityType() == ActivityType.WORKOUT && programs != null) {
            int repetitions = request.getDetail().getWorkout() == null || request.getDetail().getWorkout().getSets() == null ? 0
                : request.getDetail().getWorkout().getSets().stream().mapToInt(set -> set.getReps() == null ? 0 : set.getReps()).sum();
            programs.recordOutcome(request.getTaskId(), id, repetitions, false);
        }
        if (complete) outbox.append(
            "activity",
            id.toString(),
            new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.ACTIVITY_COMPLETED,
                1,
                0,
                Instant.now(),
                owner,
                new ActivityCompleted(
                    id,
                    request.getTaskId(),
                    request.getActivityType().name(),
                    request.getPerformedAt().toInstant(),
                    template == null ? summary(request) : templateSummary(template),
                    start == null ? null : start.toInstant(),
                    end == null ? null : end.toInstant()
                )
            )
        );
        return get(owner, id);
    }

    public ActivityResponse get(UUID owner, UUID id) {
        var rows = jdbc.query(
            SNAPSHOT_SELECT + " where user_id=? and id=?",
            this::snapshot,
            owner,
            id
        );
        if (rows.isEmpty()) throw new ApiFailure(
            404,
            "NOT_FOUND",
            "Not found",
            "Activity was not found.",
            false
        );
        return rows.getFirst();
    }

    private ActivityResponse snapshot(java.sql.ResultSet r, int index) throws java.sql.SQLException {
        var format = DetailFormat.valueOf(r.getString("detail_format"));
        ActivityTemplateSnapshot template = r.getString("template_snapshot") == null ? null
            : mapper.readValue(r.getString("template_snapshot"), ActivityTemplateSnapshot.class);
        // JSONB may contain arbitrary legacy decimals. A floating-point intermediary would corrupt revision history.
        var raw = mapper.reader().with(tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .readTree(r.getString("detail_snapshot"));
        var study = raw.get("study");
        LegacyStudyPayload legacy = null;
        if (study != null && study.isObject()) {
            var values = study.get("values");
            if (format == DetailFormat.LEGACY) legacy = new LegacyStudyPayload(LegacyStudyPayload.ProvenanceEnum.UNVERIFIED_LEGACY)
                .values(values).snapshot(study.get("snapshot"));
            var object = (tools.jackson.databind.node.ObjectNode) study;
            object.remove("values"); object.remove("snapshot");
            if (template != null) {
                var fields = object.putArray("fields");
                for (var definition : template.getFields()) {
                    var value = values == null ? null : values.get(definition.getFieldId().toString());
                    if (value == null) continue;
                    var input = fields.addObject().put("fieldId", definition.getFieldId().toString()).put("type", definition.getType().name());
                    input.set(switch (definition.getType()) {
                        case NUMBER -> "numberValue"; case TIME -> "timeSeconds"; case SHORT_TEXT -> "textValue";
                        case CHECK -> "checked"; case MEMO -> "memoValue";
                    }, value);
                }
            }
        }
        var responseDetail = mapper.treeToValue(raw, ActivityDetailResponse.class);
        if (template == null) {
            if (responseDetail.getStudy() != null) responseDetail.getStudy().setFields(null);
            if (responseDetail.getWorkout() != null) responseDetail.getWorkout().setFields(null);
            if (responseDetail.getClimbing() != null) responseDetail.getClimbing().setFields(null);
        }
        return new ActivityResponse(
            (UUID) r.getObject("id"),
            (UUID) r.getObject("command_id"),
            (UUID) r.getObject("task_id"),
            (UUID) r.getObject("user_id"),
            ActivityType.valueOf(r.getString("activity_type")),
            r.getObject("performed_at", OffsetDateTime.class),
            responseDetail,
            ActivityStatus.valueOf(r.getString("status")),
            r.getLong("revision")
        )
            .detailFormat(format).templateSnapshot(template).legacyStudyPayload(legacy)
            .note(r.getString("note"))
            .previousPerformedAt(r.getObject("previous_performed_at", OffsetDateTime.class))
            .startedAt(r.getObject("started_at", OffsetDateTime.class))
            .endedAt(r.getObject("ended_at", OffsetDateTime.class))
            .syncState(ActivitySyncState.valueOf(r.getString("sync_state")))
            .syncReason(r.getString("sync_reason"));
    }

    public ActivityPageResponse list(
        UUID owner,
        LocalDate date,
        String cursor,
        Integer limit
    ) {
        int size = limit == null ? 20 : limit;
        UUID after = null;
        try {
            if (cursor != null) after = UUID.fromString(cursor);
        } catch (IllegalArgumentException exception) {
            throw new ApiFailure(
                400,
                "INVALID_CURSOR",
                "Invalid cursor",
                "Use the returned cursor.",
                false
            );
        }
        var args = new ArrayList<Object>();
        args.add(owner);
        String sql = SNAPSHOT_SELECT + " where user_id=?";
        if (date != null) {
            sql += " and performed_at>=? and performed_at<?";
            args.add(
                date.atStartOfDay(ZoneId.of("Asia/Seoul")).toOffsetDateTime()
            );
            args.add(
                date
                    .plusDays(1)
                    .atStartOfDay(ZoneId.of("Asia/Seoul"))
                    .toOffsetDateTime()
            );
        }
        if (after != null) {
            sql += " and id>?";
            args.add(after);
        }
        sql += " order by id limit ?";
        args.add(size + 1);
        var records = jdbc.query(sql, this::snapshot, args.toArray());
        var page = new ActivityPageResponse(
            records
                .stream()
                .limit(size)
                .toList()
        );
        if (records.size() > size) page.nextCursor(records.get(size - 1).getActivityId().toString());
        return page;
    }

    public MonthlyActivitySummaryResponse monthlySummary(UUID owner, YearMonth month, ActivityType type) {
        var current = YearMonth.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        if (month.isAfter(current)) throw new ApiFailure(
            400, "FUTURE_MONTH", "Future month", "Choose the current or an earlier Seoul month.", false
        );
        var from = month.atDay(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
        var to = month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
        var row = jdbc.queryForMap("""
            with per_activity as (
              select a.id, a.status,
                case
                  when a.started_at is not null and a.ended_at is not null
                    then floor(extract(epoch from (a.ended_at-a.started_at)))::bigint
                  when a.activity_type='WORKOUT' then coalesce(
                    (select sum(s.duration_seconds)::bigint from workout_set s where s.activity_id=a.id), 0)
                  when a.activity_type='STUDY' then coalesce(
                    (select s.duration_minutes::bigint*60 from study_detail s where s.activity_id=a.id), 0)
                  when a.activity_type='CLIMBING' then coalesce(
                    (select c.duration_seconds::bigint from climbing_detail c where c.activity_id=a.id), 0)
                  else 0
                end as duration_seconds
              from activity_record a
              where a.user_id=? and a.activity_type=? and a.performed_at>=? and a.performed_at<?
            )
            select count(*) filter (where status='COMPLETED')::int as completed_count,
                   coalesce(sum(duration_seconds) filter (where status in ('COMPLETED','PARTIAL')),0)::bigint as duration_seconds
            from per_activity
            """, owner, type.name(), from, to);
        return new MonthlyActivitySummaryResponse(
            month.toString(), type, ((Number) row.get("completed_count")).intValue(),
            ((Number) row.get("duration_seconds")).longValue()
        );
    }

    @Transactional
    public ActivityResponse correct(UUID owner, UUID id, CorrectActivityRequest request) {
        jdbc.queryForList("select id from activity_record where user_id=? and id=? for update", owner, id);
        var old = get(owner, id);
        if (!old.getVersion().equals(request.getExpectedVersion())) throw fail("VERSION_CONFLICT", false);
        if (old.getStatus() == ActivityStatus.VOIDED) throw fail("ACTIVITY_VOIDED", false);
        details.validate(old.getActivityType(), request.getDetail());
        var templateValues = templates.validate(request.getDetail(), old.getTemplateSnapshot());
        var start = request.getStartedAt();
        var end = request.getEndedAt();
        var zone = ZoneId.of("Asia/Seoul");
        var date = request.getPerformedAt().atZoneSameInstant(zone).toLocalDate();
        if ((start == null) != (end == null) || (start != null &&
            (!end.isAfter(start) || !start.atZoneSameInstant(zone).toLocalDate().equals(date)
                || !end.atZoneSameInstant(zone).toLocalDate().equals(date))))
            throw new ApiFailure(400, "INVALID_INTERVAL", "Invalid interval", "Use an ordered start/end pair on the performed Seoul date.", false);
        archive(old);
        boolean complete = old.getStatus() == ActivityStatus.COMPLETED;
        jdbc.update("update activity_record set previous_performed_at=performed_at,performed_at=?,started_at=?,ended_at=?,note=?,revision=revision+1,sync_state=?,sync_reason=null where id=?",
            request.getPerformedAt(), start, end, request.getNote(), complete ? "PENDING" : "NOT_REQUIRED", id);
        if (old.getActivityType() == ActivityType.STUDY) {
            // Update only validated columns on legacy rows; their arbitrary JSON is never round-tripped through input DTOs.
            var study = request.getDetail().getStudy();
            jdbc.update("update study_detail set subject=?,duration_minutes=? where activity_id=?",
                study == null ? null : study.getSubject(), study == null ? null : study.getDurationMinutes(), id);
            if (old.getDetailFormat() == DetailFormat.TEMPLATE) jdbc.update("update study_detail set values_json=cast(? as jsonb) where activity_id=?",
                mapper.writeValueAsString(templateValues), id);
        } else details.replace(id, old.getActivityType(), request.getDetail(), old.getTemplateSnapshot(), templateValues);
        if (complete && old.getActivityType() == ActivityType.WORKOUT && programs != null) programs.recordOutcome(old.getTaskId(), id, repetitions(request.getDetail()), false);
        var summaryRequest = new CreateActivityRequest().activityType(old.getActivityType()).detail(request.getDetail());
        outbox.append("activity", id.toString(), new EventEnvelope<>(UUID.randomUUID(), EventType.ACTIVITY_CORRECTED,
            1, old.getVersion() + 1, Instant.now(), owner,
            new ActivityCorrected(id, old.getTaskId(), old.getActivityType().name(), request.getPerformedAt().toInstant(),
                old.getTemplateSnapshot() == null ? summary(summaryRequest) : templateSummary(old.getTemplateSnapshot()),
                start == null ? null : start.toInstant(), end == null ? null : end.toInstant(),
                old.getPerformedAt().toInstant(), old.getStatus().name())));
        return get(owner, id);
    }

    private void archive(ActivityResponse old) {
        jdbc.update("insert into activity_revision_history(activity_id,revision,snapshot) values (?,?,cast(? as jsonb))",
            old.getActivityId(), old.getVersion(), mapper.writeValueAsString(old));
    }

    private String templateSummary(ActivityTemplateSnapshot template) {
        return template.getName() + " · " + switch (template.getDomain()) {
            case STUDY -> "공부 기록";
            case WORKOUT -> "운동 기록";
            case CLIMBING -> "클라이밍 기록";
        };
    }

    @Transactional
    public ActivityResponse voidRecord(
        UUID owner,
        UUID id,
        VoidActivityRequest request
    ) {
        var rows = jdbc.queryForList(
            "select * from activity_record where user_id=? and id=? for update",
            owner,
            id
        );
        if (rows.isEmpty()) return get(owner, id);
        var row = rows.getFirst();
        if (
            ((Number) row.get("revision")).longValue() != request.getVersion()
        ) throw fail("VERSION_CONFLICT", false);
        if ("VOIDED".equals(row.get("status"))) return get(owner, id);
        boolean complete = "COMPLETED".equals(row.get("status"));
        archive(get(owner, id));
        jdbc.update(
            "update activity_record set status='VOIDED',revision=revision+1,void_reason=?,sync_state=?,sync_reason=null where id=?",
            request.getReason(),
            complete ? "PENDING" : "NOT_REQUIRED",
            id
        );
        if (complete && ActivityType.WORKOUT.name().equals(row.get("activity_type")) && programs != null) programs.recordOutcome((UUID) row.get("task_id"), id, 0, true);
        if (complete) outbox.append(
            "activity",
            id.toString(),
            new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.ACTIVITY_VOIDED,
                1,
                request.getVersion() + 1,
                Instant.now(),
                owner,
                new ActivityVoided(
                    id,
                    (UUID) row.get("task_id"),
                    Instant.now(),
                    request.getReason()
                )
            )
        );
        return get(owner, id);
    }

    private int repetitions(ActivityDetail detail) {
        return detail == null || detail.getWorkout() == null || detail.getWorkout().getSets() == null ? 0
            : detail.getWorkout().getSets().stream().mapToInt(set -> set.getReps() == null ? 0 : set.getReps()).sum();
    }

    private String summary(CreateActivityRequest request) {
        return switch (request.getActivityType()) {
            case WORKOUT -> "운동 기록" +
                (request.getDetail().getWorkout() == null ||
                request.getDetail().getWorkout().getSets() == null
                    ? ""
                    : " · " +
                      request.getDetail().getWorkout().getSets().size() +
                      "세트");
            case STUDY -> "공부 기록" +
                (request.getDetail().getStudy() == null ||
                request.getDetail().getStudy().getDurationMinutes() == null
                    ? ""
                    : " · " +
                      request.getDetail().getStudy().getDurationMinutes() +
                      "분");
            case CLIMBING -> "클라이밍 기록" +
                (request.getDetail().getClimbing() == null ||
                request.getDetail().getClimbing().getRounds() == null
                    ? ""
                    : " · " +
                      request.getDetail().getClimbing().getRounds().size() +
                      "라운드");
        };
    }

    private String fingerprint(CreateActivityRequest request) {
        try {
            // Canonical object keys and normalized Instant make retransmission independent of JSON member order/offset spelling.
            var tree = mapper.valueToTree(request);
            ((tools.jackson.databind.node.ObjectNode) tree).put(
                "performedAt",
                request.getPerformedAt().toInstant().toString()
            );
            if (request.getStartedAt() != null) (
                (tools.jackson.databind.node.ObjectNode) tree
            ).put("startedAt", request.getStartedAt().toInstant().toString());
            if (request.getEndedAt() != null) (
                (tools.jackson.databind.node.ObjectNode) tree
            ).put("endedAt", request.getEndedAt().toInstant().toString());
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(
                    canonical(tree).getBytes(StandardCharsets.UTF_8)
                )
            );
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String canonical(JsonNode node) {
        if (node.isObject()) {
            var keys = new TreeSet<String>();
            node.propertyNames().forEach(keys::add);
            return (
                "{" +
                keys
                    .stream()
                    .map(
                        k ->
                            mapper.writeValueAsString(k) +
                            ":" +
                            canonical(node.get(k))
                    )
                    .collect(java.util.stream.Collectors.joining(",")) +
                "}"
            );
        }
        if (node.isArray()) {
            var items = new ArrayList<String>();
            node.forEach(n -> items.add(canonical(n)));
            return "[" + String.join(",", items) + "]";
        }
        return node.toString();
    }

    private ApiFailure fail(String code, boolean retryable) {
        return new ApiFailure(
            409,
            code,
            "Conflict",
            "Reload synchronization state before retrying.",
            retryable
        );
    }
}
