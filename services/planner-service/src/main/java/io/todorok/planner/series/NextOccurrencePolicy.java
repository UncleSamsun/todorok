package io.todorok.planner.series;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Pure civil-date rules. Callers supply a reserved occurrence cursor, never a rolled date. */
@Component
public class NextOccurrencePolicy {

    public Optional<LocalDate> next(
        RecurrenceRule rule,
        LocalDate anchor,
        LocalDate end,
        LocalDate after
    ) {
        LocalDate lower = after.plusDays(1).isBefore(anchor) ? anchor : after.plusDays(1);
        Optional<LocalDate> candidate = switch (rule.frequency()) {
            case DAILY -> Optional.of(
                anchor.plusDays(
                    ((ChronoUnit.DAYS.between(anchor, lower) + rule.interval() - 1) /
                        rule.interval()) *
                        rule.interval()
                )
            );
            case WEEKLY -> weekly(rule, anchor, lower);
            case MONTHLY -> monthly(rule, anchor, lower);
        };
        return candidate.filter(date -> end == null || !date.isAfter(end));
    }

    private Optional<LocalDate> weekly(RecurrenceRule rule, LocalDate anchor, LocalDate lower) {
        LocalDate start = anchor.minusDays(anchor.getDayOfWeek().getValue() % 7);
        long week = ChronoUnit.DAYS.between(start, lower) / 7;
        long cycle = week / rule.interval();
        LocalDate weekStart = start.plusWeeks(cycle * rule.interval());
        return rule
            .weekdays()
            .stream()
            .map(day -> {
                LocalDate candidate = weekStart.plusDays(day % 7);
                return candidate.isBefore(lower) ? candidate.plusWeeks(rule.interval()) : candidate;
            })
            .min(LocalDate::compareTo);
    }

    private Optional<LocalDate> monthly(RecurrenceRule rule, LocalDate anchor, LocalDate lower) {
        YearMonth start = YearMonth.from(anchor);
        long cycle = ChronoUnit.MONTHS.between(start, YearMonth.from(lower)) / rule.interval();
        // Gregorian month/day patterns repeat after 4800 months. Impossible rules terminate.
        for (long c = cycle; c <= cycle + 4800; c++) {
            YearMonth month = start.plusMonths(c * rule.interval());
            if (rule.monthDay() <= month.lengthOfMonth()) {
                LocalDate candidate = month.atDay(rule.monthDay());
                if (!candidate.isBefore(lower)) return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public boolean shouldGenerate(boolean hasActive, boolean archived, String completion) {
        return (
            !hasActive &&
            !archived &&
            (completion.equals("COMPLETED") || completion.equals("SKIPPED"))
        );
    }

    public LocalDate scheduledDate(LocalDate occurrence, LocalDate today) {
        return occurrence.isBefore(today) ? today : occurrence;
    }
}
