package io.todorok.activity.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import io.todorok.activity.record.*;
import io.todorok.activity.api.model.*;
import io.todorok.messaging.OutboxEventWriter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.create-schemas=false",
        "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration"
})
class ActivityPersistenceIntegrationTest {

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
                            "/docker-entrypoint-initdb.d/001-create-service-roles.sh");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl()
            + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=activity");
        registry.add("spring.datasource.username", () -> "activity_app");
        registry.add("spring.datasource.password", () -> "activity-test-password");
    }

    private static Path roleScript() {
        return Path.of(System.getProperty("todorok.repository.root"))
                .resolve("infra/docker/postgres/init/001-create-service-roles.sh");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired PersistenceSampleRepository repository;
    @Autowired Flyway flyway;
    @Autowired ActivityService activities;
    @Autowired ActivityDetailStore details;
    @Autowired OutboxEventWriter outbox;
    @Autowired ObjectMapper mapper;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void summarizesEachActivityOnceAndPrefersTheHeaderInterval() {
        UUID owner = UUID.randomUUID();
        var workout = activities.create(owner, recordRequest(owner, ActivityType.WORKOUT));
        jdbc.update("update activity_record set started_at=?,ended_at=? where id=?",
            OffsetDateTime.parse("2026-09-07T10:00:00+09:00"),
            OffsetDateTime.parse("2026-09-07T10:30:00+09:00"), workout.getActivityId());
        jdbc.update("update workout_set set duration_seconds=999 where activity_id=?", workout.getActivityId());
        var partial = activities.create(owner, recordRequest(owner, ActivityType.WORKOUT)
            .completionStatus(ActivityCompletionStatus.PARTIAL));
        jdbc.update("update workout_set set duration_seconds=120 where activity_id=?", partial.getActivityId());
        var voided = activities.create(owner, recordRequest(owner, ActivityType.WORKOUT));
        activities.voidRecord(owner, voided.getActivityId(), new VoidActivityRequest("mistake", 0L));
        UUID other = UUID.randomUUID();
        activities.create(other, recordRequest(other, ActivityType.WORKOUT));

        var summary = activities.monthlySummary(owner, YearMonth.of(2026, 9), ActivityType.WORKOUT);

        assertThat(summary.getMonth()).isEqualTo("2026-09");
        assertThat(summary.getActivityType()).isEqualTo(ActivityType.WORKOUT);
        assertThat(summary.getCompletedCount()).isEqualTo(1);
        assertThat(summary.getDurationSeconds()).isEqualTo(1920L);
    }

    @Test
    void summarizesTheLeapYearSeoulMonthAcrossEquivalentOffsets() {
        UUID seoulOwner = UUID.randomUUID();
        createAt(seoulOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-02-01T00:00:00+09:00", studyDetail(1));
        createAt(seoulOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-01-31T23:59:59+09:00", studyDetail(2));
        createAt(seoulOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-02-29T23:59:59+09:00", studyDetail(4));
        createAt(seoulOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-03-01T00:00:00+09:00", studyDetail(8));

        UUID utcOwner = UUID.randomUUID();
        createAt(utcOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-01-31T15:00:00Z", studyDetail(1));
        createAt(utcOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-01-31T14:59:59Z", studyDetail(2));
        createAt(utcOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-02-29T14:59:59Z", studyDetail(4));
        createAt(utcOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-02-29T15:00:00Z", studyDetail(8));

        var seoulSummary = activities.monthlySummary(
            seoulOwner, YearMonth.of(2024, 2), ActivityType.STUDY);
        var utcSummary = activities.monthlySummary(
            utcOwner, YearMonth.of(2024, 2), ActivityType.STUDY);

        assertThat(seoulSummary.getCompletedCount()).isEqualTo(2);
        assertThat(seoulSummary.getDurationSeconds()).isEqualTo(300L);
        assertThat(utcSummary.getCompletedCount()).isEqualTo(2);
        assertThat(utcSummary.getDurationSeconds()).isEqualTo(300L);

        // Mutation control: UTC calendar bounds still count two rows but select 4+8 minutes.
        assertThat(studyDurationBetweenUtcBounds(seoulOwner)).isEqualTo(720L);
        assertThat(studyDurationBetweenUtcBounds(utcOwner)).isEqualTo(720L);
    }

    @Test
    void sumsWorkoutSetsWithoutCountingTheActivityMoreThanOnce() {
        UUID owner = UUID.randomUUID();
        var workout = new ActivityDetail().workout(new WorkoutDetail()
            .addSetsItem(new WorkoutSet().exercise("row").durationSeconds(60))
            .addSetsItem(new WorkoutSet().exercise("press").durationSeconds(120)));
        createAt(owner, ActivityType.WORKOUT, ActivityCompletionStatus.COMPLETED,
            "2024-02-10T10:00:00+09:00", workout);

        var summary = activities.monthlySummary(
            owner, YearMonth.of(2024, 2), ActivityType.WORKOUT);

        assertThat(summary.getCompletedCount()).isEqualTo(1);
        assertThat(summary.getDurationSeconds()).isEqualTo(180L);
    }

    @Test
    void summarizesStudyAndClimbingByStatusOwnerAndType() {
        UUID studyOwner = UUID.randomUUID();
        createAt(studyOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-02-03T10:00:00+09:00", studyDetail(2));
        createAt(studyOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-02-04T10:00:00+09:00", studyDetail(null));
        createAt(studyOwner, ActivityType.STUDY, ActivityCompletionStatus.PARTIAL,
            "2024-02-05T10:00:00+09:00", studyDetail(3));
        createAt(studyOwner, ActivityType.STUDY, ActivityCompletionStatus.PARTIAL,
            "2024-02-06T10:00:00+09:00", studyDetail(0));
        var voidedStudy = createAt(
            studyOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-02-07T10:00:00+09:00", studyDetail(5));
        activities.voidRecord(studyOwner, voidedStudy.getActivityId(),
            new VoidActivityRequest("summary fixture", 0L));
        createAt(UUID.randomUUID(), ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-02-08T10:00:00+09:00", studyDetail(7));
        createAt(studyOwner, ActivityType.CLIMBING, ActivityCompletionStatus.COMPLETED,
            "2024-02-09T10:00:00+09:00", climbingDetail(999));

        UUID climbingOwner = UUID.randomUUID();
        createAt(climbingOwner, ActivityType.CLIMBING, ActivityCompletionStatus.COMPLETED,
            "2024-02-03T11:00:00+09:00", climbingDetail(40));
        createAt(climbingOwner, ActivityType.CLIMBING, ActivityCompletionStatus.COMPLETED,
            "2024-02-04T11:00:00+09:00", climbingDetail(null));
        createAt(climbingOwner, ActivityType.CLIMBING, ActivityCompletionStatus.PARTIAL,
            "2024-02-05T11:00:00+09:00", climbingDetail(20));
        createAt(climbingOwner, ActivityType.CLIMBING, ActivityCompletionStatus.PARTIAL,
            "2024-02-06T11:00:00+09:00", climbingDetail(0));
        var voidedClimbing = createAt(
            climbingOwner, ActivityType.CLIMBING, ActivityCompletionStatus.COMPLETED,
            "2024-02-07T11:00:00+09:00", climbingDetail(50));
        activities.voidRecord(climbingOwner, voidedClimbing.getActivityId(),
            new VoidActivityRequest("summary fixture", 0L));
        createAt(UUID.randomUUID(), ActivityType.CLIMBING, ActivityCompletionStatus.COMPLETED,
            "2024-02-08T11:00:00+09:00", climbingDetail(70));
        createAt(climbingOwner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-02-09T11:00:00+09:00", studyDetail(4));

        var studySummary = activities.monthlySummary(
            studyOwner, YearMonth.of(2024, 2), ActivityType.STUDY);
        var climbingSummary = activities.monthlySummary(
            climbingOwner, YearMonth.of(2024, 2), ActivityType.CLIMBING);

        assertThat(studySummary.getCompletedCount()).isEqualTo(2);
        assertThat(studySummary.getDurationSeconds()).isEqualTo(300L);
        assertThat(climbingSummary.getCompletedCount()).isEqualTo(2);
        assertThat(climbingSummary.getDurationSeconds()).isEqualTo(60L);
    }

    @Test
    void floorsFractionalHeaderSecondsAndDoesNotAddDetailDuration() {
        UUID owner = UUID.randomUUID();
        var request = recordRequestAt(
            owner, ActivityType.CLIMBING, ActivityCompletionStatus.COMPLETED,
            OffsetDateTime.parse("2024-02-12T10:00:00+09:00"), climbingDetail(99))
            .startedAt(OffsetDateTime.parse("2024-02-12T10:00:00+09:00"))
            .endedAt(OffsetDateTime.parse("2024-02-12T10:00:01.900+09:00"));
        activities.create(owner, request);

        var summary = activities.monthlySummary(
            owner, YearMonth.of(2024, 2), ActivityType.CLIMBING);

        assertThat(summary.getCompletedCount()).isEqualTo(1);
        assertThat(summary.getDurationSeconds()).isEqualTo(1L);
    }

    @Test
    void repeatedAndEmptyMonthlySummaryReadsLeaveOwnerStateUnchanged() {
        UUID owner = UUID.randomUUID();
        var original = createAt(
            owner, ActivityType.STUDY, ActivityCompletionStatus.COMPLETED,
            "2024-02-15T10:00:00+09:00", studyDetail(2));
        activities.correct(owner, original.getActivityId(), new CorrectActivityRequest(
            0L, OffsetDateTime.parse("2024-02-16T10:00:00+09:00"), studyDetail(3))
            .note("summary read-only fixture"));
        UUID orphanTask = UUID.randomUUID();
        jdbc.update(
            "insert into task_reference(task_id,user_id,task_type,scheduled_date,status,version) values (?,?,?,?,?,?)",
            orphanTask, owner, ActivityType.STUDY.name(),
            java.time.LocalDate.parse("2024-02-20"), "PLANNED", 0L);
        var before = ownerState(owner);
        assertThat(before.get("task_reference")).hasSize(2);

        // Detector control is kept separate from the normal read-only calls below.
        jdbc.update("update task_reference set status='SKIPPED',version=1 where task_id=?", orphanTask);
        assertThat(ownerState(owner)).isNotEqualTo(before);
        jdbc.update("update task_reference set status='PLANNED',version=0 where task_id=?", orphanTask);
        assertThat(ownerState(owner)).isEqualTo(before);

        var first = activities.monthlySummary(
            owner, YearMonth.of(2024, 2), ActivityType.STUDY);
        assertThat(first.getCompletedCount()).isEqualTo(1);
        assertThat(first.getDurationSeconds()).isEqualTo(180L);
        assertThat(ownerState(owner)).isEqualTo(before);

        var repeated = activities.monthlySummary(
            owner, YearMonth.of(2024, 2), ActivityType.STUDY);
        assertThat(repeated.getCompletedCount()).isEqualTo(1);
        assertThat(repeated.getDurationSeconds()).isEqualTo(180L);
        assertThat(ownerState(owner)).isEqualTo(before);

        var empty = activities.monthlySummary(
            owner, YearMonth.of(2024, 1), ActivityType.STUDY);
        assertThat(empty.getCompletedCount()).isZero();
        assertThat(empty.getDurationSeconds()).isZero();
        assertThat(ownerState(owner)).isEqualTo(before);
    }

    @Test
    void readsOneRevisionWhenCorrectionCommitsBetweenResultSetAndMapping() throws Exception {
        for (ActivityType type : ActivityType.values()) {
            for (String operation : List.of("GET", "LIST", "REPLAY")) {
                UUID owner = UUID.randomUUID();
                var request = recordRequest(owner, type);
                var original = activities.create(owner, request);
                var obtained = new CountDownLatch(1);
                var resume = new CountDownLatch(1);
                var armed = new AtomicBoolean(true);
                // Pause after PostgreSQL has returned the header ResultSet, before application mapping.
                // The old implementation then queried the newly committed detail and mixed revisions.
                var barrierJdbc = new JdbcTemplate(jdbc.getDataSource()) {
                    @Override
                    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
                        return super.query(sql, (rs, index) -> {
                            if (sql.contains("*") && sql.contains("from activity_record") && armed.compareAndSet(true, false)) {
                                obtained.countDown();
                                try {
                                    if (!resume.await(15, TimeUnit.SECONDS)) throw new AssertionError("Reader barrier timed out");
                                } catch (InterruptedException e) { throw new IllegalStateException(e); }
                            }
                            return rowMapper.mapRow(rs, index);
                        }, args);
                    }
                };
                var reader = new ActivityService(barrierJdbc, details, outbox, mapper, java.time.Clock.systemUTC());
                try (var pool = Executors.newSingleThreadExecutor()) {
                    var pending = pool.submit(() -> new TransactionTemplate(transactions).execute(status -> switch (operation) {
                        case "GET" -> reader.get(owner, original.getActivityId());
                        case "LIST" -> reader.list(owner, request.getPerformedAt().toLocalDate(), null, 20).getItems().getFirst();
                        default -> reader.create(owner, request);
                    }));
                    try {
                        assertThat(obtained.await(15, TimeUnit.SECONDS)).as(type + " " + operation).isTrue();
                        activities.correct(owner, original.getActivityId(), new CorrectActivityRequest(0L,
                            request.getPerformedAt().minusDays(1), detail(type, 22)).note("new"));
                    } finally { resume.countDown(); }
                    var read = pending.get(15, TimeUnit.SECONDS);
                    assertThat(read.getVersion()).as(type + " " + operation).isZero();
                    assertThat(read.getNote()).isEqualTo("old");
                    assertThat(read.getPerformedAt()).isEqualTo(original.getPerformedAt());
                    assertThat(read.getDetail()).isEqualTo(original.getDetail());
                    var latest = activities.get(owner, original.getActivityId());
                    assertThat(latest.getVersion()).isEqualTo(1);
                    assertThat(latest.getDetail()).isNotEqualTo(original.getDetail());
                }
            }
        }
    }

    @Test
    void eightConcurrentOriginalCommandsStillReturnOneRecord() throws Exception {
        UUID owner = UUID.randomUUID();
        var request = recordRequest(owner, ActivityType.STUDY);
        var ready = new CountDownLatch(8);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(8)) {
            var results = new java.util.ArrayList<Future<ActivityResponse>>();
            for (int n = 0; n < 8; n++) results.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(15, TimeUnit.SECONDS)) throw new AssertionError("Command barrier timed out");
                return activities.create(owner, request);
            }));
            assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var ids = new java.util.HashSet<UUID>();
            for (var result : results) ids.add(result.get(20, TimeUnit.SECONDS).getActivityId());
            assertThat(ids).hasSize(1);
            assertThat(jdbc.queryForObject("select count(*) from activity_record where user_id=?", Integer.class, owner)).isEqualTo(1);
        } finally { start.countDown(); }
    }

    private CreateActivityRequest recordRequest(UUID owner, ActivityType type) {
        UUID task = UUID.randomUUID();
        jdbc.update("insert into task_reference(task_id,user_id,task_type,scheduled_date,status,version) values (?,?,?,date '2026-09-07','PLANNED',0)", task, owner, type.name());
        return new CreateActivityRequest(UUID.randomUUID(), task, type, ActivityCompletionStatus.COMPLETED,
            OffsetDateTime.parse("2026-09-07T10:00:00+09:00"), detail(type, 11)).note("old");
    }

    private ActivityResponse createAt(
        UUID owner,
        ActivityType type,
        ActivityCompletionStatus status,
        String performedAt,
        ActivityDetail detail
    ) {
        return activities.create(owner, recordRequestAt(
            owner, type, status, OffsetDateTime.parse(performedAt), detail));
    }

    private CreateActivityRequest recordRequestAt(
        UUID owner,
        ActivityType type,
        ActivityCompletionStatus status,
        OffsetDateTime performedAt,
        ActivityDetail detail
    ) {
        UUID task = UUID.randomUUID();
        var scheduledDate = performedAt.atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDate();
        jdbc.update(
            "insert into task_reference(task_id,user_id,task_type,scheduled_date,status,version) values (?,?,?,?,?,?)",
            task, owner, type.name(), scheduledDate, "PLANNED", 0L);
        // These explicit dates only build monthly aggregate fixtures; they do not exercise correction behavior.
        return new CreateActivityRequest(
            UUID.randomUUID(), task, type, status, performedAt, detail).note("summary boundary fixture");
    }

    private ActivityDetail studyDetail(Integer durationMinutes) {
        return new ActivityDetail().study(
            new StudyDetail().subject("summary fixture").durationMinutes(durationMinutes));
    }

    private ActivityDetail climbingDetail(Integer durationSeconds) {
        return new ActivityDetail().climbing(
            new ClimbingDetail().durationSeconds(durationSeconds));
    }

    private long studyDurationBetweenUtcBounds(UUID owner) {
        return jdbc.queryForObject("""
            select coalesce(sum(d.duration_minutes::bigint * 60), 0)::bigint
            from activity_record a join study_detail d on d.activity_id=a.id
            where a.user_id=? and a.activity_type='STUDY' and a.status='COMPLETED'
              and a.performed_at>=? and a.performed_at<?
            """,
            Long.class,
            owner,
            OffsetDateTime.parse("2024-02-01T00:00:00Z"),
            OffsetDateTime.parse("2024-03-01T00:00:00Z"));
    }

    private Map<String, List<String>> ownerState(UUID owner) {
        var state = new LinkedHashMap<String, List<String>>();
        state.put("activity_record", jsonRows(
            "select a.* from activity_record a where a.user_id=? order by a.id", owner));
        state.put("task_reference", jsonRows("""
            select t.* from task_reference t where t.user_id=? order by t.task_id
            """, owner));
        state.put("workout_detail", jsonRows("""
            select d.* from workout_detail d join activity_record a on a.id=d.activity_id
            where a.user_id=? order by d.activity_id
            """, owner));
        state.put("workout_set", jsonRows("""
            select d.* from workout_set d join activity_record a on a.id=d.activity_id
            where a.user_id=? order by d.activity_id,d.position
            """, owner));
        state.put("study_detail", jsonRows("""
            select d.* from study_detail d join activity_record a on a.id=d.activity_id
            where a.user_id=? order by d.activity_id
            """, owner));
        state.put("climbing_detail", jsonRows("""
            select d.* from climbing_detail d join activity_record a on a.id=d.activity_id
            where a.user_id=? order by d.activity_id
            """, owner));
        state.put("climbing_round", jsonRows("""
            select d.* from climbing_round d join activity_record a on a.id=d.activity_id
            where a.user_id=? order by d.activity_id,d.position
            """, owner));
        state.put("activity_revision_history", jsonRows("""
            select h.* from activity_revision_history h join activity_record a on a.id=h.activity_id
            where a.user_id=? order by h.activity_id,h.revision
            """, owner));
        state.put("outbox_event", jsonRows("""
            select o.* from outbox_event o join activity_record a on o.aggregateid=a.id::text
            where a.user_id=? order by o.id
            """, owner));
        return state;
    }

    private List<String> jsonRows(String ownerQuery, UUID owner) {
        return jdbc.queryForList(
            "select to_jsonb(owner_row)::text from (" + ownerQuery + ") owner_row",
            String.class,
            owner);
    }

    private ActivityDetail detail(ActivityType type, int value) {
        return switch (type) {
            case WORKOUT -> new ActivityDetail().workout(new WorkoutDetail().addSetsItem(new WorkoutSet().exercise("squat").reps(value)));
            case STUDY -> new ActivityDetail().study(new StudyDetail().subject("math").durationMinutes(value));
            case CLIMBING -> new ActivityDetail().climbing(new ClimbingDetail().durationSeconds(value).addRoundsItem(new ClimbingRound().grade("V3").attempts(value).completed(true)));
        };
    }

    @Test
    void migratesOnlyActivitySchema() {
        assertThat(jdbc.queryForObject("select current_user", String.class))
                .isEqualTo("activity_app");
        assertThat(jdbc.queryForObject(
                "select has_database_privilege(current_user, current_database(), 'CREATE')",
                Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject(
                "select has_schema_privilege(current_user, 'planner', 'USAGE')",
                Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject(
                "select service_name from activity.service_metadata",
                String.class)).isEqualTo("activity-service");
        assertThat(jdbc.queryForObject(
                "select count(*) from activity.flyway_schema_history "
                        + "where success and version is not null",
                Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForList(
                "select schema_name from information_schema.schemata "
                        + "where schema_name in ('planner','activity','notification') "
                        + "order by schema_name",
                String.class)).isEqualTo(List.of("activity"));
    }

    @Test
    void rerunningFlywayMakesNoChanges() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from activity.flyway_schema_history "
                        + "where success and version is not null",
                Integer.class)).isEqualTo(8);
    }

    @Test
    void createsOutboxAndInboxConstraints() {
        assertThat(jdbc.queryForList(
                "select column_name from information_schema.columns "
                        + "where table_schema = 'activity' and table_name = 'outbox_event' "
                        + "order by ordinal_position",
                String.class)).containsExactly(
                        "id", "aggregatetype", "aggregateid", "type",
                        "payload", "occurred_at", "created_at");
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints "
                        + "where table_schema = 'activity' and table_name = 'processed_event' "
                        + "and constraint_type = 'PRIMARY KEY'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @Transactional
    void savesAndUpdatesOptimisticVersion() {
        var saved = repository.saveAndFlush(new PersistenceSample("activity-value"));
        assertThat(saved.version()).isZero();
        saved.rename("activity-renamed");
        repository.flush();
        assertThat(saved.version()).isEqualTo(1L);
    }
}
