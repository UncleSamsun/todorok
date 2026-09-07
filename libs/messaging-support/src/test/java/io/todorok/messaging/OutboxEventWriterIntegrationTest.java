package io.todorok.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.todorok.contracts.EventEnvelope;
import io.todorok.contracts.EventType;
import io.todorok.contracts.events.TaskScheduled;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
class OutboxEventWriterIntegrationTest {

    private static final UUID EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID TASK_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000102");

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private OutboxEventWriter writer;

    @BeforeEach
    void setUp() {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        writer = new JdbcOutboxEventWriter(
                jdbc,
                JsonMapper.builder().findAndAddModules().build());

        jdbc.execute("drop table if exists outbox_event");
        jdbc.execute("drop table if exists domain_marker");
        jdbc.execute("create table domain_marker(id integer primary key)");
        jdbc.execute("""
                create table outbox_event (
                    id uuid primary key,
                    aggregatetype varchar(80) not null,
                    aggregateid varchar(255) not null,
                    type varchar(100) not null,
                    payload jsonb not null,
                    occurred_at timestamptz not null,
                    created_at timestamptz not null default now()
                )
                """);
    }

    @Test
    void commitsDomainMarkerAndEnvelopeTogether() {
        transaction.executeWithoutResult(status -> {
            jdbc.update("insert into domain_marker(id) values (?)", 1);
            writer.append("task", TASK_ID.toString(), envelope(EVENT_ID));
        });

        assertThat(jdbc.queryForObject(
                "select payload ->> 'eventId' from outbox_event where id = ?",
                String.class,
                EVENT_ID)).isEqualTo(EVENT_ID.toString());
        assertThat(jdbc.queryForObject(
                "select count(*) from domain_marker", Integer.class)).isEqualTo(1);
    }

    @Test
    void rollsBackDomainMarkerAndOutboxTogether() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            jdbc.update("insert into domain_marker(id) values (?)", 1);
            writer.append("task", TASK_ID.toString(), envelope(EVENT_ID));
            throw new IllegalStateException("rollback-probe");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject(
                "select count(*) from domain_marker", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from outbox_event", Integer.class)).isZero();
    }

    @Test
    void rejectsDuplicateEventIdAndRollsBackCallerWork() {
        writer.append("task", TASK_ID.toString(), envelope(EVENT_ID));

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            jdbc.update("insert into domain_marker(id) values (?)", 1);
            writer.append("task", TASK_ID.toString(), envelope(EVENT_ID));
        })).isInstanceOf(DuplicateKeyException.class);

        assertThat(jdbc.queryForObject(
                "select count(*) from domain_marker", Integer.class)).isZero();
    }

    @Test
    void rejectsBlankAggregateValuesBeforeInsert() {
        assertThatThrownBy(() -> writer.append(" ", TASK_ID.toString(), envelope(EVENT_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("aggregateType must not be blank");
        assertThatThrownBy(() -> writer.append("task", "", envelope(EVENT_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("aggregateId must not be blank");
        assertThat(jdbc.queryForObject(
                "select count(*) from outbox_event", Integer.class)).isZero();
    }

    private EventEnvelope<TaskScheduled> envelope(UUID eventId) {
        return new EventEnvelope<>(
                eventId,
                EventType.TASK_SCHEDULED,
                1,
                1,
                Instant.parse("2026-09-02T00:00:00Z"),
                UUID.fromString("00000000-0000-0000-0000-000000000103"),
                new TaskScheduled(
                        TASK_ID,
                        "WORKOUT",
                        LocalDate.parse("2026-09-02"),
                        "PLANNED"));
    }
}
