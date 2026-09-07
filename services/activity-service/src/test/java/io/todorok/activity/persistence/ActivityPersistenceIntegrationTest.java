package io.todorok.activity.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
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
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
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
                Integer.class)).isEqualTo(4);
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
                Integer.class)).isEqualTo(4);
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
