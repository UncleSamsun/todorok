package io.todorok.planner.auth;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class BootstrapCommand {
    private BootstrapCommand() {}

    public static void main(String[] args) {
        try (var context = new SpringApplicationBuilder(BootstrapConfiguration.class)
                .profiles("bootstrap").web(WebApplicationType.NONE).run(args)) {
            var env = context.getEnvironment();
            context.getBean(UserAccount.class).bootstrap(env.getRequiredProperty("TODOROK_BOOTSTRAP_EMAIL"),
                    env.getRequiredProperty("TODOROK_BOOTSTRAP_PASSWORD"), new BCryptPasswordEncoder(12));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("bootstrap")
    @EnableAutoConfiguration(exclude = {HibernateJpaAutoConfiguration.class, DataJpaRepositoriesAutoConfiguration.class,
            org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration.class})
    public static class BootstrapConfiguration {
        @Bean UserAccount userAccount(JdbcTemplate jdbc) { return new UserAccount(jdbc); }
    }
}
