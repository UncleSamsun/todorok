package io.todorok.planner.preference;

import io.todorok.web.ApiFailure;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserPreference {
    public record Value(String theme, long revision) {}
    private final JdbcTemplate jdbc;
    public UserPreference(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public Value get(UUID owner) {
        return jdbc.query("select theme,revision from planner.user_preference where user_id=?", (row, index) -> new Value(row.getString(1), row.getLong(2)), owner)
            .stream().findFirst().orElse(new Value("SYSTEM", 0));
    }

    @Transactional
    public Value save(UUID owner, String theme, long expectedRevision) {
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))", "user-preference:" + owner);
        var current = jdbc.query("select theme,revision from planner.user_preference where user_id=? for update", (row, index) -> new Value(row.getString(1), row.getLong(2)), owner);
        if (current.isEmpty()) {
            if (expectedRevision != 0) throw conflict();
            jdbc.update("insert into planner.user_preference(user_id,theme,revision) values (?,?,0)", owner, theme);
            return new Value(theme, 0);
        }
        if (current.getFirst().revision() != expectedRevision) throw conflict();
        jdbc.update("update planner.user_preference set theme=?,revision=revision+1,updated_at=now() where user_id=?", theme, owner);
        return new Value(theme, expectedRevision + 1);
    }

    private ApiFailure conflict() { return new ApiFailure(409, "VERSION_CONFLICT", "Conflict", "Reload preferences before saving.", false); }
}
