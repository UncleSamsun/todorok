package io.todorok.migration.planner;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class PlannerMigrationApplicationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    @Test
    void runsFlywayWithoutJpaKafkaOrWebServer() {
        createSchema("planner");
        try (var context = PlannerMigrationApplication.run(connectionArguments())) {
            assertThat(context.getBeansOfType(Flyway.class)).hasSize(1);
            assertThat(context.getBeansOfType(EntityManagerFactory.class)).isEmpty();
            assertThat(context.getBeansOfType(KafkaAdmin.class)).isEmpty();
            assertThat(context).isNotInstanceOf(WebServerApplicationContext.class);
            var jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            assertThat(jdbc.queryForObject(
                    "select service_name from planner.service_metadata",
                    String.class)).isEqualTo("planner-service");
        }
    }

    private String[] connectionArguments() {
        return new String[] {
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword()
        };
    }

    private void createSchema(String schema) {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("create schema " + schema);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
