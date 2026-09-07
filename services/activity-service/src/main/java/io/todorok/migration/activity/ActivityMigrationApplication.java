package io.todorok.migration.activity;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = {
        org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class
})
public class ActivityMigrationApplication {

    public static ConfigurableApplicationContext run(String... args) {
        return new SpringApplicationBuilder(ActivityMigrationApplication.class)
                .profiles("migration")
                .web(WebApplicationType.NONE)
                .run(args);
    }

    public static void main(String[] args) {
        try (var ignored = run(args)) {
        }
    }
}
