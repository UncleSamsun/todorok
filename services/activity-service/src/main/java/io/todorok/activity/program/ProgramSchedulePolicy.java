package io.todorok.activity.program;

import java.time.LocalDate;
import java.util.Set;

/** Date-level cadence for the installed three-session builtin variants. */
public final class ProgramSchedulePolicy {
    private static final Set<String> THREE_DAY_PROGRAMS = Set.of(
        "hundred-pushups-level-1", "hundred-pushups-level-2", "hundred-pushups-level-3", "recon-ron-pullups");

    public LocalDate nextDate(String catalogKey, int sessionsPerWeek, int completedSession,
                             int nextSession, LocalDate performedDate, LocalDate today) {
        int gap = 1;
        if (THREE_DAY_PROGRAMS.contains(catalogKey) && sessionsPerWeek == 3) {
            int[] offsets = {0, 2, 4};
            gap = offsets[nextSession - 1] - offsets[completedSession - 1];
            if (nextSession <= completedSession) gap += 7;
            gap = Math.max(2, gap);
        }
        LocalDate date = performedDate.plusDays(gap);
        return date.isBefore(today) ? today : date;
    }
}
