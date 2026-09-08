package io.todorok.activity.program;

import java.util.List;

public final class ProgramProgressPolicy {
    public enum Outcome { SUCCESS, FAILURE, VOIDED }
    public record Attempt(long sequence, Outcome outcome) {}
    public record Progress(int week, int session, boolean completed) {}

    public Progress calculate(int startWeek, int totalWeeks, int sessionsPerWeek, List<Attempt> history) {
        if (startWeek < 1 || totalWeeks < startWeek || sessionsPerWeek < 1) throw new IllegalArgumentException("Invalid program bounds");
        int week = startWeek;
        int slot = 0;
        boolean failed = false;
        var ordered = history.stream().filter(attempt -> attempt.outcome() != Outcome.VOIDED)
            .sorted(java.util.Comparator.comparingLong(Attempt::sequence)).toList();
        for (var attempt : ordered) {
            failed |= attempt.outcome() == Outcome.FAILURE;
            slot++;
            if (slot == sessionsPerWeek) {
                if (!failed) week++;
                if (week > totalWeeks) return new Progress(totalWeeks, sessionsPerWeek, true);
                slot = 0;
                failed = false;
            }
        }
        return new Progress(week, slot + 1, false);
    }
}
