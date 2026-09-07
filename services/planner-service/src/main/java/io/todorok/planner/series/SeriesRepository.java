package io.todorok.planner.series;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface SeriesRepository extends JpaRepository<TaskSeries, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TaskSeries s where s.id=:id and s.userId=:owner")
    Optional<TaskSeries> lock(UUID owner, UUID id);

    Optional<TaskSeries> findByUserIdAndId(UUID owner, UUID id);
}
