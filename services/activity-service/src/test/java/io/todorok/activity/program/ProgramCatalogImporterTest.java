package io.todorok.activity.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ProgramCatalogImporterTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProgramCatalogImporter importer = new ProgramCatalogImporter(mapper);
    private Path fixture(String name) { return Path.of(System.getProperty("todorok.repository.root"), "contracts", "fixtures", "catalog", name); }

    @Test void readsSyntheticCatalogWithContiguousWeeksAndExactSetTotals() {
        var catalog = importer.read(fixture("program-v1-valid.json"));
        assertThat(catalog.catalogKey()).isEqualTo("synthetic-pushup");
        assertThat(catalog.source().label()).isEqualTo("테스트 전용 합성 자료");
        assertThat(catalog.source().conditions()).containsExactly("테스트용");
        assertThat(catalog.sessionsPerWeek()).isEqualTo(3);
        assertThat(catalog.weeks()).hasSize(2);
        assertThat(catalog.weeks().getFirst().sessions().getFirst().sets()).containsExactly(3, 3, 2);
    }

    @Test void rejectsChecksumChangesAndIncorrectSetTotals() throws Exception {
        var valid = mapper.readTree(java.nio.file.Files.readString(fixture("program-v1-valid.json")));
        ((tools.jackson.databind.node.ObjectNode) valid.get("weeks").get(0).get("sessions").get(0)).put("targetTotal", 99);
        assertThatThrownBy(() -> importer.parse(valid)).hasMessageContaining("checksum");
        assertThatThrownBy(() -> importer.read(fixture("program-v1-invalid.json"))).hasMessageContaining("targetTotal");
    }

    @Test void canonicalChecksumIgnoresObjectMemberOrderButNotProgramContent() throws Exception {
        var valid = mapper.readTree(java.nio.file.Files.readString(fixture("program-v1-valid.json")));
        assertThat(importer.checksum(valid)).isEqualTo(valid.get("checksum").asText());
        ((tools.jackson.databind.node.ObjectNode) valid.get("weeks").get(1).get("sessions").get(2)).put("targetTotal", 10);
        assertThat(importer.checksum(valid)).isNotEqualTo(valid.get("checksum").asText());
    }

    @Test void requiresOriginalLinkAndSafetyMetadataForPrivateVerifiedCatalogs() throws Exception {
        var valid = (tools.jackson.databind.node.ObjectNode) mapper.readTree(java.nio.file.Files.readString(fixture("program-v1-valid.json")));
        ((tools.jackson.databind.node.ObjectNode) valid.get("source")).put("kind", "PRIVATE_VERIFIED");
        valid.put("checksum", importer.checksum(valid));

        assertThatThrownBy(() -> importer.parse(valid)).hasMessageContaining("source url");
    }
}
