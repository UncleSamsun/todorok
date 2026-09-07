package io.todorok.planner.task;

import io.todorok.planner.api.model.*;
import io.todorok.web.ApiFailure;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class TaskTransitionPolicy {

    public TaskStatus next(TaskType type, TaskStatus status, String command, boolean hasActive) {
        if (
            type != TaskType.GENERAL &&
            (command.equals("COMPLETED") ||
                (command.equals("REOPENED") && status == TaskStatus.COMPLETED))
        ) throw conflict("ACTIVITY_REQUIRED");
        return switch (command) {
            case "COMPLETED", "SKIPPED" -> {
                if (status != TaskStatus.PLANNED) throw conflict("INVALID_STATE");
                yield TaskStatus.valueOf(command);
            }
            case "REOPENED" -> {
                if (status != TaskStatus.COMPLETED && status != TaskStatus.SKIPPED) throw conflict(
                    "INVALID_STATE"
                );
                if (hasActive) throw conflict("ACTIVE_OCCURRENCE_EXISTS");
                yield TaskStatus.PLANNED;
            }
            case "DELETED" -> TaskStatus.DELETED;
            default -> throw new IllegalArgumentException("Unknown task command: " + command);
        };
    }

    public boolean rollover(TaskStatus status, LocalDate scheduled, LocalDate today) {
        return status == TaskStatus.PLANNED && scheduled.isBefore(today);
    }

    private ApiFailure conflict(String code) {
        return new ApiFailure(
            409,
            code,
            "Conflict",
            "Reload the task or use its activity record.",
            false
        );
    }
}
