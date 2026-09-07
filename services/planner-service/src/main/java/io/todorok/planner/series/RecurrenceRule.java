package io.todorok.planner.series;

import java.util.Set;

public record RecurrenceRule(
    Frequency frequency,
    int interval,
    Set<Integer> weekdays,
    int monthDay
) {
    public enum Frequency {
        DAILY,
        WEEKLY,
        MONTHLY,
    }
}
