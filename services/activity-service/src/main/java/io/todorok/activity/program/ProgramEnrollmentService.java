package io.todorok.activity.program;

import io.todorok.web.ApiFailure;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProgramEnrollmentService {
    public record Enrollment(UUID id, String catalogKey, long catalogVersion, int recommendedWeek, int startWeek, int currentWeek, int currentSession, UUID sessionId, List<Integer> targetSets) {}
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ProgramCatalogImporter importer;
    public ProgramEnrollmentService(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; this.importer = new ProgramCatalogImporter(mapper); }

    @Transactional
    public Enrollment enroll(UUID owner, UUID commandId, String key, long version, int initialTestValue, Integer startWeekOverride) {
        if (initialTestValue < 0) throw invalid("INITIAL_TEST_INVALID", "Initial test value must be non-negative.");
        var existing = jdbc.queryForList("select id,request_fingerprint from program_enrollment where user_id=? and command_id=? for update", owner, commandId);
        String fingerprint = fingerprint(key, version, initialTestValue, startWeekOverride);
        if (!existing.isEmpty()) {
            if (!fingerprint.equals(existing.getFirst().get("request_fingerprint"))) throw invalid("COMMAND_CONFLICT", "A command ID cannot change enrollment input.");
            return read((UUID) existing.getFirst().get("id"));
        }
        var rows = jdbc.queryForList("select definition::text as definition from program_catalog where catalog_key=? and catalog_version=? for update", key, version);
        if (rows.isEmpty()) throw new ApiFailure(404, "PROGRAM_NOT_FOUND", "Program not found", "Catalog key and version were not found.", false);
        var catalog = importer.parse(mapper.readTree(String.valueOf(rows.getFirst().get("definition"))));
        int recommended = 1;
        int start = startWeekOverride == null ? recommended : startWeekOverride;
        if (start < 1 || start > catalog.weeks().size()) throw invalid("START_WEEK_INVALID", "Start week is outside this catalog.");
        var first = catalog.weeks().get(start - 1).sessions().getFirst();
        UUID id = UUID.randomUUID(), sessionId = UUID.randomUUID();
        jdbc.update("insert into program_enrollment(id,user_id,catalog_key,catalog_version,command_id,request_fingerprint,initial_test_value,recommended_week,start_week,current_week,current_session,status) values (?,?,?,?,?,?,?,?,?,?,?,'ACTIVE')",
            id, owner, key, version, commandId, fingerprint, initialTestValue, recommended, start, start, first.session());
        jdbc.update("insert into program_session(id,enrollment_id,cycle,week,session,target_sets) values (?,?,1,?,?,cast(? as jsonb))",
            sessionId, id, start, first.session(), mapper.writeValueAsString(first.sets()));
        return new Enrollment(id, key, version, recommended, start, start, first.session(), sessionId, first.sets());
    }

    private Enrollment read(UUID id) {
        var enrollment = jdbc.queryForMap("select * from program_enrollment where id=?", id);
        var session = jdbc.queryForMap("select id,target_sets::text as target_sets from program_session where enrollment_id=? and cycle=1 and session=?", id, enrollment.get("current_session"));
        var sets = mapper.readTree(String.valueOf(session.get("target_sets")));
        var targets = new ArrayList<Integer>(); sets.forEach(value -> targets.add(value.asInt()));
        return new Enrollment(id, (String) enrollment.get("catalog_key"), ((Number) enrollment.get("catalog_version")).longValue(), ((Number) enrollment.get("recommended_week")).intValue(), ((Number) enrollment.get("start_week")).intValue(), ((Number) enrollment.get("current_week")).intValue(), ((Number) enrollment.get("current_session")).intValue(), (UUID) session.get("id"), List.copyOf(targets));
    }
    private String fingerprint(String key, long version, int initial, Integer override) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((key + "|" + version + "|" + initial + "|" + Objects.toString(override, "-")).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private ApiFailure invalid(String code, String detail) { return new ApiFailure(400, code, "Invalid program enrollment", detail, false); }
}
