package io.todorok.planner.task;

import static org.assertj.core.api.Assertions.*;

import io.todorok.planner.api.model.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TaskTransitionPolicyTest {

    final TaskTransitionPolicy policy = new TaskTransitionPolicy();

    @Test
    void explicitStateTransitionsGuardActivityAndExistingActive() {
        assertThat(policy.next(TaskType.GENERAL, TaskStatus.PLANNED, "COMPLETED", false)).isEqualTo(
            TaskStatus.COMPLETED
        );
        assertThat(policy.next(TaskType.STUDY, TaskStatus.PLANNED, "SKIPPED", false)).isEqualTo(
            TaskStatus.SKIPPED
        );
        assertThat(policy.next(TaskType.STUDY, TaskStatus.SKIPPED, "REOPENED", false)).isEqualTo(
            TaskStatus.PLANNED
        );
        assertThat(
            policy.next(TaskType.GENERAL, TaskStatus.COMPLETED, "REOPENED", false)
        ).isEqualTo(TaskStatus.PLANNED);
        assertThat(policy.next(TaskType.WORKOUT, TaskStatus.PLANNED, "DELETED", false)).isEqualTo(
            TaskStatus.DELETED
        );
        for (TaskType type : TaskType.values())
            for (TaskStatus status : TaskStatus.values())
                for (String command : java.util.List.of(
                    "COMPLETED",
                    "SKIPPED",
                    "REOPENED",
                    "DELETED",
                    "UNKNOWN"
                )) {
                    boolean allowed = switch (command) {
                        case "COMPLETED" -> type == TaskType.GENERAL &&
                            status == TaskStatus.PLANNED;
                        case "SKIPPED" -> status == TaskStatus.PLANNED;
                        case "REOPENED" -> status == TaskStatus.SKIPPED ||
                            (type == TaskType.GENERAL && status == TaskStatus.COMPLETED);
                        case "DELETED" -> true;
                        default -> false;
                    };
                    if (!allowed) assertThatThrownBy(() ->
                        policy.next(type, status, command, false)
                    ).isInstanceOf(RuntimeException.class);
                }
        assertThatThrownBy(() ->
            policy.next(TaskType.GENERAL, TaskStatus.COMPLETED, "REOPENED", true)
        ).isInstanceOf(io.todorok.web.ApiFailure.class);
    }

    @Test
    void rolloverOnlyMovesOverduePlanned() {
        var today = LocalDate.parse("2026-09-09");
        assertThat(policy.rollover(TaskStatus.PLANNED, today.minusDays(1), today)).isTrue();
        assertThat(policy.rollover(TaskStatus.PLANNED, today, today)).isFalse();
        assertThat(policy.rollover(TaskStatus.PLANNED, today.plusDays(1), today)).isFalse();
        for (var status : java.util.List.of(
            TaskStatus.COMPLETED,
            TaskStatus.SKIPPED,
            TaskStatus.DELETED
        ))
            assertThat(policy.rollover(status, today.minusDays(1), today)).isFalse();
    }
}
