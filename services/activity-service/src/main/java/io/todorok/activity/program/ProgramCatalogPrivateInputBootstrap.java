package io.todorok.activity.program;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ProgramCatalogPrivateInputBootstrap {
    @Bean
    ApplicationRunner privateProgramCatalogs(
        ProgramCatalogStore catalogs,
        ObjectMapper mapper,
        @Value("${todorok.program-catalog.private-inputs:}") String inputs
    ) {
        return runner(catalogs, mapper, inputs);
    }

    ApplicationRunner runner(ProgramCatalogStore catalogs, ObjectMapper mapper, String inputs) {
        return arguments -> {
            if (inputs == null || inputs.isBlank()) return;
            for (String value : inputs.split(",", -1)) {
                Path input = Path.of(value.trim());
                if (!input.isAbsolute()) throw new IllegalStateException("Program catalog input must be an absolute path.");
                if (!Files.isRegularFile(input)) throw new IllegalStateException("Program catalog input is not readable: " + input);
                try (var stream = Files.newInputStream(input)) {
                    catalogs.importCatalog(mapper.readTree(stream));
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to read program catalog input: " + input, exception);
                }
            }
        };
    }
}
