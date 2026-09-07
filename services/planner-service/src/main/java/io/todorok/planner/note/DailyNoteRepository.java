package io.todorok.planner.note;

import io.todorok.web.ApiFailure;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DailyNoteRepository {

    private final JdbcTemplate jdbc;

    public DailyNoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DailyNote get(UUID owner, LocalDate date) {
        return jdbc
            .query(
                "select content, version from planner.daily_note where user_id=? and date=?",
                (rs, row) -> new DailyNote(date, rs.getString(1), rs.getLong(2)),
                owner,
                date
            )
            .stream()
            .findFirst()
            .orElse(new DailyNote(date, "", null));
    }

    public DailyNote save(UUID owner, LocalDate date, String content, Long expectedVersion) {
        // RETURNING captures this write, even if another client writes immediately after it.
        // ON CONFLICT avoids aborting the transaction during concurrent first writes.
        var saved =
            expectedVersion == null
                ? jdbc.query(
                      "insert into planner.daily_note(user_id,date,content,version) values (?,?,?,0) on conflict (user_id,date) do nothing returning version",
                      (rs, row) -> new DailyNote(date, content, rs.getLong(1)),
                      owner,
                      date,
                      content
                  )
                : jdbc.query(
                      "update planner.daily_note set content=?,version=version+1 where user_id=? and date=? and version=? returning version",
                      (rs, row) -> new DailyNote(date, content, rs.getLong(1)),
                      content,
                      owner,
                      date,
                      expectedVersion
                  );
        return saved
            .stream()
            .findFirst()
            .orElseThrow(() ->
                new ApiFailure(
                    409,
                    "VERSION_CONFLICT",
                    "Conflict",
                    "Reload the note version before retrying.",
                    false
                )
            );
    }
}
