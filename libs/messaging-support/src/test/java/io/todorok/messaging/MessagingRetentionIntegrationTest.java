package io.todorok.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.PGProperty;
import org.postgresql.core.BaseConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class MessagingRetentionIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.11-alpine")
            .withCommand(
                    "postgres",
                    "-c", "wal_level=logical",
                    "-c", "max_replication_slots=1",
                    "-c", "max_wal_senders=1");

    private JdbcTemplate jdbc;
    private String maintenanceSql;

    @BeforeEach
    void setUp() throws Exception {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("drop publication if exists retention_publication");
        jdbc.execute("select pg_drop_replication_slot(slot_name) "
                + "from pg_replication_slots where slot_name = 'todorok_outbox_slot'");
        jdbc.execute("drop schema if exists planner cascade");
        jdbc.execute("drop schema if exists activity cascade");
        jdbc.execute("drop schema if exists notification cascade");
        jdbc.execute("create schema planner");
        jdbc.execute("create schema activity");
        jdbc.execute("create schema notification");
        createMessagingTables("planner", true);
        createMessagingTables("activity", true);
        createMessagingTables("notification", false);
        jdbc.execute("create publication retention_publication for table planner.outbox_event");
        var root = Path.of(System.getProperty("todorok.repository.root"));
        maintenanceSql = Files.readString(root.resolve(
                "infra/docker/postgres/maintenance/prune-messaging.sql"));
    }

    @Test
    void prunesOnlyRowsOlderThanRetentionBoundaryWhenSlotIsCaughtUp() throws Exception {
        insertRows();
        try (var replicationConnection = replicationConnection()) {
            var pgConnection = replicationConnection.unwrap(BaseConnection.class);
            pgConnection.getReplicationAPI()
                    .createReplicationSlot()
                    .logical()
                    .withSlotName("todorok_outbox_slot")
                    .withOutputPlugin("pgoutput")
                    .make();
            try (var stream = pgConnection.getReplicationAPI()
                    .replicationStream()
                    .logical()
                    .withSlotName("todorok_outbox_slot")
                    .withSlotOption("proto_version", 1)
                    .withSlotOption("publication_names", "retention_publication")
                    .withStatusInterval(1, TimeUnit.SECONDS)
                    .start()) {
                jdbc.execute(maintenanceSql);
            }
        }

        assertThat(count("planner.outbox_event")).isEqualTo(1);
        assertThat(count("activity.outbox_event")).isEqualTo(1);
        assertThat(count("planner.processed_event")).isEqualTo(1);
        assertThat(count("activity.processed_event")).isEqualTo(1);
        assertThat(count("notification.processed_event")).isEqualTo(1);
    }

    @Test
    void refusesToPruneWithoutActiveSlot() {
        insertRows();
        assertThatThrownBy(() -> jdbc.execute(maintenanceSql))
                .isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining("todorok_outbox_slot does not exist");
        assertThat(count("planner.outbox_event")).isEqualTo(2);
    }

    private java.sql.Connection replicationConnection() throws Exception {
        var properties = new Properties();
        PGProperty.USER.set(properties, POSTGRES.getUsername());
        PGProperty.PASSWORD.set(properties, POSTGRES.getPassword());
        PGProperty.REPLICATION.set(properties, "database");
        PGProperty.PREFER_QUERY_MODE.set(properties, "simple");
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(properties, "9.4");
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), properties);
    }

    private void createMessagingTables(String schema, boolean outbox) {
        if (outbox) {
            jdbc.execute("create table " + schema + ".outbox_event ("
                    + "id uuid primary key, created_at timestamptz not null)");
        }
        jdbc.execute("create table " + schema + ".processed_event ("
                + "event_id uuid primary key, event_type varchar(100) not null, "
                + "processed_at timestamptz not null)");
    }

    private void insertRows() {
        for (var schema : new String[] {"planner", "activity"}) {
            jdbc.update("insert into " + schema + ".outbox_event(id, created_at) "
                    + "values (?, now() - interval '8 days'), (?, now())",
                    UUID.randomUUID(), UUID.randomUUID());
        }
        for (var schema : new String[] {"planner", "activity", "notification"}) {
            jdbc.update("insert into " + schema + ".processed_event("
                            + "event_id, event_type, processed_at) "
                            + "values (?, 'TEST', now() - interval '31 days'), "
                            + "(?, 'TEST', now())",
                    UUID.randomUUID(), UUID.randomUUID());
        }
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
