package io.todorok.activity.program;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.ObjectMapper;

@Configuration
@Profile("local")
public class ProgramCatalogBootstrap {
    @Bean
    ApplicationRunner syntheticProgramCatalog(ProgramCatalogStore catalogs, ObjectMapper mapper) {
        return arguments -> {
            try (var input = getClass().getResourceAsStream("/catalog/synthetic-program-v1.json")) {
                if (input == null) throw new IllegalStateException("Synthetic program catalog resource is missing.");
                catalogs.importCatalog(mapper.readTree(input));
            }
        };
    }
}
