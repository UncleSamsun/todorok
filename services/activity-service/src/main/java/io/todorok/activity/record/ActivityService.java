package io.todorok.activity.record;

import io.todorok.activity.api.model.*;
import io.todorok.contracts.*;
import io.todorok.contracts.events.*;
import io.todorok.messaging.OutboxEventWriter;
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

    private final JdbcTemplate jdbc;
    private final ActivityDetailStore details;
    private final OutboxEventWriter outbox;
    private final ObjectMapper mapper;

    public ActivityService(
        JdbcTemplate jdbc,
        ActivityDetailStore details,
        OutboxEventWriter outbox,
        ObjectMapper mapper
    ) {
        this.jdbc = jdbc;
        this.details = details;
        this.outbox = outbox;
        this.mapper = mapper;
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
            insert into activity_record(id,user_id,task_id,activity_type,performed_at,status,command_id,fingerprint,note,sync_state,started_at,ended_at)
            values (?,?,?,?,?,?,?,?,?,?,?,?)
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
            end
        );
        details.save(id, request.getActivityType(), request.getDetail());
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
                    summary(request),
                    start == null ? null : start.toInstant(),
                    end == null ? null : end.toInstant()
                )
            )
        );
        return get(owner, id);
    }

    public ActivityResponse get(UUID owner, UUID id) {
        var rows = jdbc.query(
            "select * from activity_record where user_id=? and id=?",
            (r, n) -> {
                var type = ActivityType.valueOf(r.getString("activity_type"));
                return new ActivityResponse(
                    id,
                    (UUID) r.getObject("command_id"),
                    (UUID) r.getObject("task_id"),
                    owner,
                    type,
                    r.getObject("performed_at", OffsetDateTime.class),
                    details.read(id, type),
                    ActivityStatus.valueOf(r.getString("status")),
                    r.getLong("revision")
                )
                    .note(r.getString("note"))
                    .previousPerformedAt(r.getObject("previous_performed_at", OffsetDateTime.class))
                    .startedAt(r.getObject("started_at", OffsetDateTime.class))
                    .endedAt(r.getObject("ended_at", OffsetDateTime.class))
                    .syncState(
                        ActivitySyncState.valueOf(r.getString("sync_state"))
                    )
                    .syncReason(r.getString("sync_reason"));
            },
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
        String sql = "select id from activity_record where user_id=?";
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
        var ids = jdbc.queryForList(sql, UUID.class, args.toArray());
        var page = new ActivityPageResponse(
            ids
                .stream()
                .limit(size)
                .map(id -> get(owner, id))
                .toList()
        );
        if (ids.size() > size) page.nextCursor(ids.get(size - 1).toString());
        return page;
    }

    @Transactional
    public ActivityResponse correct(UUID owner, UUID id, CorrectActivityRequest request) {
        jdbc.queryForList("select id from activity_record where user_id=? and id=? for update", owner, id);
        var old = get(owner, id);
        if (!old.getVersion().equals(request.getExpectedVersion())) throw fail("VERSION_CONFLICT", false);
        if (old.getStatus() == ActivityStatus.VOIDED) throw fail("ACTIVITY_VOIDED", false);
        details.validate(old.getActivityType(), request.getDetail());
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
        details.replace(id, old.getActivityType(), request.getDetail());
        var summaryRequest = new CreateActivityRequest().activityType(old.getActivityType()).detail(request.getDetail());
        outbox.append("activity", id.toString(), new EventEnvelope<>(UUID.randomUUID(), EventType.ACTIVITY_CORRECTED,
            1, old.getVersion() + 1, Instant.now(), owner,
            new ActivityCorrected(id, old.getTaskId(), old.getActivityType().name(), request.getPerformedAt().toInstant(),
                summary(summaryRequest), start == null ? null : start.toInstant(), end == null ? null : end.toInstant(),
                old.getPerformedAt().toInstant(), old.getStatus().name())));
        return get(owner, id);
    }

    private void archive(ActivityResponse old) {
        jdbc.update("insert into activity_revision_history(activity_id,revision,snapshot) values (?,?,cast(? as jsonb))",
            old.getActivityId(), old.getVersion(), mapper.writeValueAsString(old));
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
