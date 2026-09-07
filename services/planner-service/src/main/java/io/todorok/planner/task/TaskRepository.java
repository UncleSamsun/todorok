package io.todorok.planner.task;

import io.todorok.planner.api.model.TaskResponse;
import io.todorok.planner.api.model.TaskType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    @Query(
        "select t from Task t where t.id=:id and t.userId=:owner and t.status<>io.todorok.planner.api.model.TaskStatus.DELETED"
    )
    Optional<Task> owned(UUID owner, UUID id);

    @Query(
        "select new io.todorok.planner.api.model.TaskResponse(t.id,t.userId,t.title,t.taskType,t.scheduledDate,t.status,t.version) from Task t where t.userId=:owner and t.scheduledDate=:date and t.status<>io.todorok.planner.api.model.TaskStatus.DELETED order by t.id"
    )
    List<TaskResponse> day(UUID owner, LocalDate date);

    @Query(
        "select new io.todorok.planner.api.model.TaskResponse(t.id,t.userId,t.title,t.taskType,t.scheduledDate,t.status,t.version) from Task t where t.userId=:owner and t.id=:id and t.status<>io.todorok.planner.api.model.TaskStatus.DELETED"
    )
    Optional<TaskResponse> detail(UUID owner, UUID id);

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
