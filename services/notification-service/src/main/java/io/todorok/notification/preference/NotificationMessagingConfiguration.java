package io.todorok.notification.preference;

import io.todorok.messaging.InboxEventGuard;
import io.todorok.messaging.JdbcInboxEventGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class NotificationMessagingConfiguration {
    @Bean InboxEventGuard notificationInbox(JdbcTemplate jdbc) { return new JdbcInboxEventGuard(jdbc); }
}
