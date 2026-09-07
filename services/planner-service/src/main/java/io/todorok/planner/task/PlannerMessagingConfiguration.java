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
    org.springframework.kafka.listener.CommonErrorHandler plannerFailureHandler(
        org.springframework.kafka.core.KafkaTemplate<Object, Object> kafka
    ) {
        return new io.todorok.messaging.DefaultConsumerFailureHandlerFactory().create(
            kafka,
            "todorok.activity.v1.dlt"
        );
    }

    @Bean
    io.todorok.messaging.InboxEventGuard plannerInbox(JdbcTemplate jdbc) {
        return new io.todorok.messaging.JdbcInboxEventGuard(jdbc);
    }

    @Bean
    java.time.Clock plannerClock() {
        return java.time.Clock.systemUTC();
    }

    @Bean
    OutboxEventWriter outboxEventWriter(
        JdbcTemplate jdbc,
        ObjectMapper mapper
    ) {
        return new JdbcOutboxEventWriter(jdbc, mapper);
    }
}
