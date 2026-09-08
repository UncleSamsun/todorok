package io.todorok.activity.program;

import io.todorok.web.ApiFailure;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProgramCatalogStore {
    public enum ImportResult { IMPORTED, UNCHANGED }
    public record Imported(ProgramCatalogImporter.Catalog catalog, ImportResult result) {}
    public record Summary(String catalogKey, long catalogVersion, String checksum, String name, ProgramCatalogImporter.Source source, int sessionsPerWeek, int totalWeeks) {}
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ProgramCatalogImporter importer;

    public ProgramCatalogStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.importer = new ProgramCatalogImporter(mapper);
    }

    @Transactional
    public Imported importCatalog(JsonNode source) {
        var catalog = importer.parse(source);
        var existing = jdbc.queryForList("select checksum from program_catalog where catalog_key=? and catalog_version=? for update", catalog.catalogKey(), catalog.version());
        if (!existing.isEmpty()) {
            String checksum = (String) existing.getFirst().get("checksum");
            if (checksum.equals(catalog.checksum())) return new Imported(catalog, ImportResult.UNCHANGED);
            throw new ApiFailure(409, "CATALOG_VERSION_CONFLICT", "Catalog version conflict", "A catalog key and version cannot change its checksum.", false);
        }
        jdbc.update("insert into program_catalog(catalog_key,catalog_version,checksum,name,source,definition) values (?,?,?,?,cast(? as jsonb),cast(? as jsonb))",
            catalog.catalogKey(), catalog.version(), catalog.checksum(), catalog.name(), mapper.writeValueAsString(source.get("source")), mapper.writeValueAsString(source));
        return new Imported(catalog, ImportResult.IMPORTED);
    }

    @Transactional(readOnly = true)
    public java.util.List<Summary> list() {
        return jdbc.query("select catalog_key,catalog_version,checksum,name,source::text as source,(definition->>'sessionsPerWeek')::int as sessions_per_week,jsonb_array_length(definition->'weeks') as total_weeks from program_catalog order by catalog_key,catalog_version desc",
            (row, index) -> new Summary(row.getString("catalog_key"), row.getLong("catalog_version"), row.getString("checksum"), row.getString("name"), importer.parseSource(mapper.readTree(row.getString("source"))), row.getInt("sessions_per_week"), row.getInt("total_weeks")));
    }
}
