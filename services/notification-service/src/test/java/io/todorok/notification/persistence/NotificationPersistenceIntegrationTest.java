package io.todorok.notification.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
class NotificationPersistenceIntegrationTest {

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
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "notification_app");
        registry.add("spring.datasource.password", () -> "notification-test-password");
        registry.add("spring.datasource.hikari.connection-init-sql", () -> "set search_path to notification");
    }

    private static Path roleScript() {
        return Path.of(System.getProperty("todorok.repository.root"))
                .resolve("infra/docker/postgres/init/001-create-service-roles.sh");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired PersistenceSampleRepository repository;
    @Autowired Flyway flyway;
    @Autowired io.todorok.notification.preference.NotificationPreferenceConsumer preferences;
    @Autowired tools.jackson.databind.ObjectMapper mapper;

    @Test
    void migratesOnlyNotificationSchema() {
        assertThat(jdbc.queryForObject("select current_user", String.class))
                .isEqualTo("notification_app");
        assertThat(jdbc.queryForObject(
                "select has_database_privilege(current_user, current_database(), 'CREATE')",
                Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject(
                "select has_schema_privilege(current_user, 'planner', 'USAGE')",
                Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject(
                "select service_name from notification.service_metadata",
                String.class)).isEqualTo("notification-service");
        assertThat(jdbc.queryForObject(
                "select count(*) from notification.flyway_schema_history "
                        + "where success and version is not null",
                Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForList(
                "select schema_name from information_schema.schemata "
                        + "where schema_name in ('planner','activity','notification') "
                        + "order by schema_name",
                String.class)).isEqualTo(List.of("notification"));
    }

    @Test
    void rerunningFlywayMakesNoChanges() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from notification.flyway_schema_history "
                        + "where success and version is not null",
                Integer.class)).isEqualTo(4);
    }

    @Test
    void createsOnlyInboxConstraints() {
        assertThat(jdbc.queryForList(
                "select column_name from information_schema.columns "
                        + "where table_schema = 'notification' "
                        + "and table_name = 'processed_event' "
                        + "order by ordinal_position",
                String.class)).containsExactly("event_id", "event_type", "processed_at");
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = 'notification' "
                        + "and table_name = 'outbox_event'",
                Integer.class)).isZero();
    }

    @Test
    void consumesPreferenceOnceAndKeepsTheHighestRevision() throws Exception {
        UUID owner = UUID.randomUUID();
        preferences.receive(event(owner, UUID.randomUUID(), 2, true, "09:30"));
        preferences.receive(event(owner, UUID.randomUUID(), 1, false, "08:00"));
        preferences.receive(event(owner, UUID.randomUUID(), 2, true, "09:30"));
        assertThat(jdbc.queryForObject("select notifications_enabled from notification_preference where user_id=?", Boolean.class, owner)).isTrue();
        assertThat(jdbc.queryForObject("select summary_time from notification_preference where user_id=?", String.class, owner).trim()).isEqualTo("09:30");
        assertThat(jdbc.queryForObject("select revision from notification_preference where user_id=?", Long.class, owner)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("select count(*) from processed_event where event_type='NOTIFICATION_PREFERENCE_CHANGED'", Integer.class)).isEqualTo(3);
    }

    private String event(UUID owner, UUID eventId, long revision, boolean enabled, String time) throws Exception {
        return mapper.writeValueAsString(new io.todorok.contracts.EventEnvelope<>(eventId,
            io.todorok.contracts.EventType.NOTIFICATION_PREFERENCE_CHANGED, 1, revision, Instant.now(), owner,
            Map.of("notificationsEnabled", enabled, "summaryTime", time)));
    }

    @Test
    @Transactional
    void savesAndUpdatesOptimisticVersion() {
        var saved = repository.saveAndFlush(new PersistenceSample("notification-value"));
        assertThat(saved.version()).isZero();
        saved.rename("notification-renamed");
        repository.flush();
        assertThat(saved.version()).isEqualTo(1L);
    }
}
