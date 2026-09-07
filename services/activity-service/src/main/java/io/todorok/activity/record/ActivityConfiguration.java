package io.todorok.activity.record;

import io.todorok.messaging.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;

@Configuration
public class ActivityConfiguration {

    @Bean
    Clock activityClock() {
        return Clock.systemUTC();
    }

    @Bean
    OutboxEventWriter activityOutbox(JdbcTemplate jdbc, ObjectMapper mapper) {
        return new JdbcOutboxEventWriter(jdbc, mapper);
    }

    @Bean
    InboxEventGuard activityInbox(JdbcTemplate jdbc) {
        return new JdbcInboxEventGuard(jdbc);
    }

    @Bean
    org.springframework.kafka.listener.CommonErrorHandler activityFailureHandler(
        org.springframework.kafka.core.KafkaTemplate<Object, Object> kafka
    ) {
        return new DefaultConsumerFailureHandlerFactory().create(
            kafka,
            "todorok.task.v1.dlt"
        );
    }
}
