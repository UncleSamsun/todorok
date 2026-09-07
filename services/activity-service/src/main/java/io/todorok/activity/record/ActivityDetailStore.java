package io.todorok.activity.record;

import io.todorok.activity.api.model.*;
import io.todorok.web.ApiFailure;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Detail boundaries intentionally stay relational except Study values and snapshots. */
@Component
public class ActivityDetailStore {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ActivityDetailStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void validate(ActivityType type, ActivityDetail detail) {
        if (
            detail == null ||
            (detail.getWorkout() != null && type != ActivityType.WORKOUT) ||
            (detail.getStudy() != null && type != ActivityType.STUDY) ||
            (detail.getClimbing() != null && type != ActivityType.CLIMBING)
        ) throw new ApiFailure(
            400,
            "DETAIL_TYPE_MISMATCH",
            "Invalid detail",
            "Use detail matching the activity type.",
            false
        );
        if (
            (detail.getWorkout() != null &&
                detail.getWorkout().getSets() != null &&
                detail.getWorkout().getSets().stream().anyMatch(Objects::isNull)) ||
            (detail.getClimbing() != null &&
                detail.getClimbing().getRounds() != null &&
                detail.getClimbing().getRounds().stream().anyMatch(Objects::isNull))
        ) throw new ApiFailure(
            400,
            "INVALID_DETAIL_ITEM",
            "Invalid detail",
            "Sets and rounds must be objects.",
            false
        );
    }

    public void replace(UUID id, ActivityType type, ActivityDetail detail) {
        // The caller archives the old header and typed detail before replacing current rows.
        for (String table : List.of("workout_set", "climbing_round", "workout_detail", "study_detail", "climbing_detail"))
            jdbc.update("delete from " + table + " where activity_id=?", id);
        save(id, type, detail);
    }

    public void save(UUID id, ActivityType type, ActivityDetail detail) {
        save(id, type, detail, null);
    }

    public void save(UUID id, ActivityType type, ActivityDetail detail, Map<String,Object> studyValues) {
        switch (type) {
            case WORKOUT -> {
                jdbc.update(
                    "insert into workout_detail(activity_id) values (?)",
                    id
                );
                if (
                    detail.getWorkout() != null &&
                    detail.getWorkout().getSets() != null
                ) {
                    int index = 0;
                    for (var set : detail.getWorkout().getSets())
                        jdbc.update(
                            "insert into workout_set values (?,?,?,?,?,?)",
                            id,
                            index++,
                            set.getExercise(),
                            set.getReps(),
                            set.getWeightKg(),
                            set.getDurationSeconds()
                        );
                }
            }
            case STUDY -> {
                var d = detail.getStudy();
                jdbc.update(
                    "insert into study_detail values (?,?,?,cast(? as jsonb),cast(? as jsonb))",
                    id,
                    d == null ? null : d.getSubject(),
                    d == null ? null : d.getDurationMinutes(),
                    studyValues == null
                        ? null
                        : mapper.writeValueAsString(studyValues),
                    null
                );
            }
            case CLIMBING -> {
                jdbc.update(
                    "insert into climbing_detail(activity_id,duration_seconds) values (?,?)",
                    id,
                    detail.getClimbing() == null
                        ? null
                        : detail.getClimbing().getDurationSeconds()
                );
                if (
                    detail.getClimbing() != null &&
                    detail.getClimbing().getRounds() != null
                ) {
                    int index = 0;
                    for (var round : detail.getClimbing().getRounds())
                        jdbc.update(
                            "insert into climbing_round values (?,?,?,?,?)",
                            id,
                            index++,
                            round.getGrade(),
                            round.getAttempts(),
                            round.getCompleted()
                        );
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public ActivityDetail read(UUID id, ActivityType type) {
        return switch (type) {
            case WORKOUT -> new ActivityDetail().workout(
                new WorkoutDetail().sets(
                    jdbc.query(
                        "select * from workout_set where activity_id=? order by position",
                        (r, n) ->
                            new WorkoutSet()
                                .exercise(r.getString("exercise"))
                                .reps((Integer) r.getObject("reps"))
                                .weightKg(r.getBigDecimal("weight_kg"))
                                .durationSeconds(
                                    (Integer) r.getObject("duration_seconds")
                                ),
                        id
                    )
                )
            );
            case CLIMBING -> new ActivityDetail().climbing(
                new ClimbingDetail()
                    .durationSeconds(
                        jdbc.queryForObject(
                            "select duration_seconds from climbing_detail where activity_id=?",
                            Integer.class,
                            id
                        )
                    )
                    .rounds(
                        jdbc.query(
                            "select * from climbing_round where activity_id=? order by position",
                            (r, n) ->
                                new ClimbingRound()
                                    .grade(r.getString("grade"))
                                    .attempts((Integer) r.getObject("attempts"))
                                    .completed(
                                        (Boolean) r.getObject("completed")
                                    ),
                            id
                        )
                    )
            );
            case STUDY -> new ActivityDetail().study(
                jdbc.queryForObject(
                    "select * from study_detail where activity_id=?",
                    (r, n) ->
                        new StudyDetail()
                            .subject(r.getString("subject"))
                            .durationMinutes(
                                (Integer) r.getObject("duration_minutes")
                            ),
                    id
                )
            );
        };
    }
}
