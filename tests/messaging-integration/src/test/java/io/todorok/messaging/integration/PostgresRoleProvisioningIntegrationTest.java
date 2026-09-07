package io.todorok.messaging.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@Testcontainers
class PostgresRoleProvisioningIntegrationTest {

    private static final String SCRIPT = "/docker-entrypoint-initdb.d/001-create-service-roles.sh";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.11-alpine")
            .withDatabaseName("todorok")
            .withUsername("postgres")
            .withPassword("admin-password")
            .withEnv("PLANNER_DB_PASSWORD", "planner-one")
            .withEnv("ACTIVITY_DB_PASSWORD", "activity-one")
            .withEnv("NOTIFICATION_DB_PASSWORD", "notification-one")
            .withEnv("DEBEZIUM_DB_PASSWORD", "debezium-one")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(repositoryRoot().resolve(
                            "infra/docker/postgres/init/001-create-service-roles.sh")),
                    SCRIPT);

    @Test
    void reconcilesExistingRolesWithoutBroadeningPrivilegesAndRotatesOnDemand()
            throws Exception {
        executeAs("planner_app", "planner-one", "create table planner.outbox_event(id uuid)");
        executeAs("planner_app", "planner-one", "create table planner.private_task(id uuid)");
        executeAs("activity_app", "activity-one",
                "create table activity.service_access_probe(id uuid)");
        executeAs("planner_app", "planner-one", "grant select on planner.outbox_event to debezium_app");

        assertThat(runProvision(false, "planner-one").getExitCode()).isZero();
        assertThatThrownBy(() -> executeAs(
                "planner_app", "planner-one", "select * from activity.service_access_probe"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> executeAs(
                "debezium_app", "debezium-one", "select * from planner.private_task"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> executeAs(
                "debezium_app", "debezium-one", "create schema forbidden"))
                .isInstanceOf(SQLException.class);
        assertThat(queryAs(
                "debezium_app", "debezium-one", "select count(*) from planner.outbox_event"))
                .isZero();

        assertThat(runProvision(true, "planner-two").getExitCode()).isZero();
        assertThatThrownBy(() -> queryAs(
                "planner_app", "planner-one", "select count(*) from planner.outbox_event"))
                .isInstanceOf(SQLException.class);
        assertThat(queryAs(
                "planner_app", "planner-two", "select count(*) from planner.outbox_event"))
                .isZero();
    }

    private static org.testcontainers.containers.Container.ExecResult runProvision(
            boolean updateCredentials,
            String plannerPassword) throws Exception {
        var environment = List.of(
                "POSTGRES_HOST=localhost",
                "POSTGRES_DB=todorok",
                "POSTGRES_USER=postgres",
                "POSTGRES_PASSWORD=admin-password",
                "PLANNER_DB_PASSWORD=" + plannerPassword,
                "ACTIVITY_DB_PASSWORD=activity-one",
                "NOTIFICATION_DB_PASSWORD=notification-one",
                "DEBEZIUM_DB_PASSWORD=debezium-one",
                "DATABASE_CREDENTIAL_UPDATE=" + updateCredentials);
        var command = new java.util.ArrayList<String>();
        command.add("env");
        command.addAll(environment);
        command.add("bash");
        command.add(SCRIPT);
        return POSTGRES.execInContainer(command.toArray(String[]::new));
    }

    private static void executeAs(String user, String password, String sql) throws SQLException {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), user, password);
                var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static int queryAs(String user, String password, String sql) throws SQLException {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), user, password);
                var statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("todorok.repository.root"));
    }
}
