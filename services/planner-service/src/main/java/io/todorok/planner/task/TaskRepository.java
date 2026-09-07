package io.todorok.planner.task;

import io.todorok.planner.api.model.TaskType;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Task t where t.id=:id and t.userId=:owner")
    Optional<Task> lockIncludingDeleted(UUID owner, UUID id);

    @Query("select t.seriesId from Task t where t.id=:id and t.userId=:owner")
    Optional<UUID> seriesId(UUID owner, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select t from Task t where t.id=:id and t.userId=:owner and t.status<>io.todorok.planner.api.model.TaskStatus.DELETED"
    )
    Optional<Task> lock(UUID owner, UUID id);

    @Query(
        "select count(t)>0 from Task t where t.seriesId=:series and t.status=io.todorok.planner.api.model.TaskStatus.PLANNED"
    )
    boolean hasActive(UUID series);

    @Query(
        "select t.id from Task t where t.userId=:owner and t.status=io.todorok.planner.api.model.TaskStatus.PLANNED and t.scheduledDate<:today order by t.id"
    )
    List<UUID> overdue(UUID owner, LocalDate today);

    @Query(
        "select t from Task t where t.id=:id and t.userId=:owner and t.status<>io.todorok.planner.api.model.TaskStatus.DELETED"
    )
    Optional<Task> owned(UUID owner, UUID id);

    @Query(
        "select new io.todorok.planner.task.TaskView(t.id,t.userId,t.title,t.taskType,t.scheduledDate,t.status,t.version,t.seriesId,t.occurrenceDate,t.note,t.activityId,t.performedAt,t.completionSummary,t.startedAt,t.endedAt) from Task t where t.userId=:owner and t.scheduledDate=:date and t.status<>io.todorok.planner.api.model.TaskStatus.DELETED order by t.id"
    )
    List<TaskView> day(UUID owner, LocalDate date);

    @Query(
        "select new io.todorok.planner.task.TaskView(t.id,t.userId,t.title,t.taskType,t.scheduledDate,t.status,t.version,t.seriesId,t.occurrenceDate,t.note,t.activityId,t.performedAt,t.completionSummary,t.startedAt,t.endedAt) from Task t where t.userId=:owner and t.id=:id and t.status<>io.todorok.planner.api.model.TaskStatus.DELETED"
    )
    Optional<TaskView> detail(UUID owner, UUID id);

    interface Count {
        LocalDate getDate();
        TaskType getType();
        Long getTotal();
        Long getCompleted();
    }

    @Query(
        "select t.scheduledDate as date,t.taskType as type,count(t) as total,sum(case when t.status=io.todorok.planner.api.model.TaskStatus.COMPLETED then 1 else 0 end) as completed from Task t where t.userId=:owner and t.scheduledDate between :from and :to and t.status<>io.todorok.planner.api.model.TaskStatus.DELETED group by t.scheduledDate,t.taskType"
    )
    List<Count> counts(UUID owner, LocalDate from, LocalDate to);
}
