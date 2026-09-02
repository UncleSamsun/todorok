package io.todorok.planner.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.create-schemas=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration"
})
class PlannerPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    @Autowired JdbcTemplate jdbc;
    @Autowired PersistenceSampleRepository repository;
    @Autowired Flyway flyway;

    @Test
    void migratesOnlyPlannerSchema() {
        assertThat(jdbc.queryForObject(
                "select service_name from planner.service_metadata",
                String.class)).isEqualTo("planner-service");
        assertThat(jdbc.queryForObject(
                "select count(*) from planner.flyway_schema_history "
                        + "where success and version is not null",
                Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList(
                "select schema_name from information_schema.schemata "
                        + "where schema_name in ('planner','activity','notification') "
                        + "order by schema_name",
                String.class)).isEqualTo(List.of("planner"));
    }

    @Test
    void rerunningFlywayMakesNoChanges() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from planner.flyway_schema_history "
                        + "where success and version is not null",
                Integer.class)).isEqualTo(2);
    }

    @Test
    @Transactional
    void savesAndUpdatesOptimisticVersion() {
        var saved = repository.saveAndFlush(new PersistenceSample("planner-value"));
        assertThat(saved.version()).isZero();
        saved.rename("planner-renamed");
        repository.flush();
        assertThat(saved.version()).isEqualTo(1L);
        assertThat(repository.findById(saved.id())).get()
                .extracting(PersistenceSample::name)
                .isEqualTo("planner-renamed");
    }
}
