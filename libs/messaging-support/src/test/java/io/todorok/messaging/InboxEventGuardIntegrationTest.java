package io.todorok.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class InboxEventGuardIntegrationTest {

    private static final UUID EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private InboxEventGuard guard;

    @BeforeEach
    void setUp() {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        guard = new JdbcInboxEventGuard(jdbc);

        jdbc.execute("drop table if exists processed_event");
        jdbc.execute("drop table if exists local_counter");
        jdbc.execute("""
                create table processed_event (
                    event_id uuid primary key,
                    event_type varchar(100) not null,
                    processed_at timestamptz not null default now()
                )
                """);
        jdbc.execute("create table local_counter(value integer not null)");
        jdbc.update("insert into local_counter(value) values (0)");
    }

    @Test
    void claimsEachEventOnlyOnce() {
        assertThat(guard.claim(EVENT_ID, "TASK_CHANGED")).isTrue();
        assertThat(guard.claim(EVENT_ID, "TASK_CHANGED")).isFalse();
        assertThat(jdbc.queryForObject(
                "select count(*) from processed_event", Integer.class)).isEqualTo(1);
    }

    @Test
    void rollbackAllowsRedelivery() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            assertThat(guard.claim(EVENT_ID, "TASK_CHANGED")).isTrue();
            jdbc.update("update local_counter set value = value + 1");
            throw new IllegalStateException("rollback-probe");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(guard.claim(EVENT_ID, "TASK_CHANGED")).isTrue();
        assertThat(jdbc.queryForObject(
                "select value from local_counter", Integer.class)).isZero();
    }

    @Test
    void rejectsInvalidIdentityBeforeInsert() {
        assertThatThrownBy(() -> guard.claim(null, "TASK_CHANGED"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("eventId must not be null");
        assertThatThrownBy(() -> guard.claim(EVENT_ID, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventType must not be blank");
        assertThat(jdbc.queryForObject(
                "select count(*) from processed_event", Integer.class)).isZero();
    }
}
