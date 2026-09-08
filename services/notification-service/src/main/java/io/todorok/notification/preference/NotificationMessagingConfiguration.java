package io.todorok.notification.preference;

import io.todorok.messaging.InboxEventGuard;
import io.todorok.messaging.JdbcInboxEventGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;

@Configuration
@EnableKafka
public class NotificationMessagingConfiguration {
    @Bean InboxEventGuard notificationInbox(JdbcTemplate jdbc) { return new JdbcInboxEventGuard(jdbc); }
    @Bean ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        var properties = Map.<String, Object>of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(properties));
        return factory;
    }
}
