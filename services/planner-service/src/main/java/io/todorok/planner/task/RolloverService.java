package io.todorok.planner.task;

import io.todorok.planner.api.model.*;
import io.todorok.planner.series.SeriesRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolloverService {

    private final TaskRepository tasks;
    private final SeriesRepository series;
    private final TaskEvents events;
    private final Clock clock;
    private final TaskTransitionPolicy policy;

    public RolloverService(
        TaskRepository tasks,
        SeriesRepository series,
        TaskEvents events,
        Clock clock,
        TaskTransitionPolicy policy
    ) {
        this.tasks = tasks;
        this.series = series;
        this.events = events;
        this.clock = clock;
        this.policy = policy;
    }

    @Transactional
    public RolloverResponse rollover(UUID owner) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        var ids = tasks.overdue(owner, today);
        // All series locks precede all task locks, in stable order across commands.
        ids.stream()
            .map(id -> tasks.seriesId(owner, id))
            .flatMap(Optional::stream)
            .distinct()
            .sorted()
            .forEach(id -> series.lock(owner, id));
        int count = 0;
        for (UUID id : ids) {
            var found = tasks.lock(owner, id);
            if (found.isEmpty()) continue;
            Task task = found.get();
            if (policy.rollover(task.status, task.scheduledDate, today)) {
                task.scheduledDate = today;
                tasks.flush();
                events.publish(task, "ROLLED_OVER");
                count++;
            }
        }
        return new RolloverResponse(today, count);
    }
}
