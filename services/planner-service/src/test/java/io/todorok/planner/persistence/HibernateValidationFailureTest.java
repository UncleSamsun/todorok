package io.todorok.planner.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.todorok.planner.PlannerApplication;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class HibernateValidationFailureTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    @Test
    void rejectsMissingMappedTable() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(PlannerApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword(),
                        "--spring.flyway.enabled=true",
                        "--spring.flyway.create-schemas=true",
                        "--spring.flyway.locations=classpath:db/migration"))
                .hasRootCauseInstanceOf(SchemaManagementException.class)
                .hasStackTraceContaining(
                        "Schema validation: missing table [planner.persistence_sample]");
    }
}
