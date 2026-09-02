package io.todorok.migration.notification;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = {
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class
})
public class NotificationMigrationApplication {

    public static ConfigurableApplicationContext run(String... args) {
        return new SpringApplicationBuilder(NotificationMigrationApplication.class)
                .profiles("migration")
                .web(WebApplicationType.NONE)
                .run(args);
    }

    public static void main(String[] args) {
        try (var ignored = run(args)) {
        }
    }
}
