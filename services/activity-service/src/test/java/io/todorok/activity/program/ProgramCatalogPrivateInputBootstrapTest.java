package io.todorok.activity.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

class ProgramCatalogPrivateInputBootstrapTest {
    @TempDir Path directory;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void importsEveryConfiguredAbsolutePrivateInput() throws Exception {
        Path first = copyFixture("pushup.json"), second = copyFixture("pullup.json");
        var catalogs = mock(ProgramCatalogStore.class);

        new ProgramCatalogPrivateInputBootstrap().runner(catalogs, mapper,
            first.toAbsolutePath() + "," + second.toAbsolutePath()).run(null);

        verify(catalogs, times(2)).importCatalog(any());
    }

    @Test void rejectsRelativeOrMissingPrivateInputInsteadOfSilentlySkippingIt() {
        var catalogs = mock(ProgramCatalogStore.class);

        assertThatThrownBy(() -> new ProgramCatalogPrivateInputBootstrap().runner(catalogs, mapper, "catalog.json").run(null))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("absolute");
        assertThatThrownBy(() -> new ProgramCatalogPrivateInputBootstrap().runner(catalogs, mapper,
            directory.resolve("missing.json").toAbsolutePath().toString()).run(null))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("not readable");
        verifyNoInteractions(catalogs);
    }

    @Test void skipsImportWhenNoPrivateInputIsConfigured() throws Exception {
        var catalogs = mock(ProgramCatalogStore.class);

        new ProgramCatalogPrivateInputBootstrap().runner(catalogs, mapper, "  ").run(null);

        verifyNoInteractions(catalogs);
    }

    private Path copyFixture(String name) throws Exception {
        Path target = directory.resolve(name);
        Files.copy(Path.of(System.getProperty("todorok.repository.root"), "contracts", "fixtures", "catalog", "program-v1-valid.json"), target);
        return target;
    }
}
