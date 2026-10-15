package io.todorok.activity.program;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class BuiltinProgramCatalogBootstrap {
    @Bean
    ApplicationRunner builtinProgramCatalogs(ProgramCatalogStore catalogs, ObjectMapper mapper) {
        return arguments -> {
            for (String name : java.util.List.of("hundred-pushups-level-1", "hundred-pushups-level-2", "hundred-pushups-level-3", "recon-ron-pullups")) {
                try (var input = getClass().getResourceAsStream("/catalog/builtin/" + name + ".json")) {
                    if (input == null) throw new IllegalStateException("Builtin program catalog is missing: " + name);
                    catalogs.importCatalog(mapper.readTree(input));
                }
            }
        };
    }
}
