package io.todorok.planner.task;

import io.todorok.planner.api.model.TaskResponse;
import io.todorok.planner.api.model.TaskStatus;
import io.todorok.planner.api.model.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "task", schema = "planner")
public class Task {

    @Id
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(nullable = false, length = 120)
    String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    TaskType taskType;

    @Column(name = "scheduled_date", nullable = false)
    LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    TaskStatus status;

    @Version
    @Column(nullable = false)
    Long version;

    protected Task() {}

    Task(UUID userId, String title, TaskType type, LocalDate date) {
        id = UUID.randomUUID();
        this.userId = userId;
        this.title = title;
        taskType = type;
        scheduledDate = date;
        status = TaskStatus.PLANNED;
    }

    TaskResponse response() {
        return new TaskResponse(id, userId, title, taskType, scheduledDate, status, version);
    }
}
