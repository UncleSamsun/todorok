package io.todorok.planner.task;

import io.todorok.messaging.JdbcOutboxEventWriter;
import io.todorok.messaging.OutboxEventWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class PlannerMessagingConfiguration {

    @Bean
    OutboxEventWriter outboxEventWriter(JdbcTemplate jdbc, ObjectMapper mapper) {
        return new JdbcOutboxEventWriter(jdbc, mapper);
    }
}
