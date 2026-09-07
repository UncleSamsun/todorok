package io.todorok.planner.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

class BootstrapIsolationTest {
    @Test void bootstrapDoesNotStartKafkaOrJpa() {
        new ApplicationContextRunner()
                .withUserConfiguration(BootstrapCommand.BootstrapConfiguration.class)
                .withPropertyValues("spring.profiles.active=bootstrap", "spring.flyway.enabled=false",
                        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/not-used",
                        "spring.datasource.username=test", "spring.datasource.password=test")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(UserAccount.class);
                    assertThat(context.getBeansOfType(KafkaAdmin.class)).isEmpty();
                    assertThat(context.getBeansOfType(KafkaTemplate.class)).isEmpty();
                    assertThat(context.getBeansOfType(jakarta.persistence.EntityManagerFactory.class)).isEmpty();
                });
    }
}
