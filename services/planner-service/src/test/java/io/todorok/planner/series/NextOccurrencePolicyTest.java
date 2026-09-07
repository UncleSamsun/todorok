package io.todorok.planner.series;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NextOccurrencePolicyTest {

    LocalDate d(String value) {
        return LocalDate.parse(value);
    }

    final NextOccurrencePolicy policy = new NextOccurrencePolicy();

    @Test
    void dailyUsesAnchorAcrossYearAndEndIsInclusive() {
        var rule = new RecurrenceRule(RecurrenceRule.Frequency.DAILY, 2, Set.of(), 1);
        assertThat(policy.next(rule, d("2026-12-31"), null, d("2026-12-01"))).contains(
            d("2026-12-31")
        );
        assertThat(policy.next(rule, d("2026-12-31"), null, d("2026-12-30"))).contains(
            d("2026-12-31")
        );
        assertThat(policy.next(rule, d("2026-12-31"), null, d("2026-12-31"))).contains(
            d("2027-01-02")
        );
        assertThat(policy.next(rule, d("2026-12-31"), d("2027-01-02"), d("2027-01-01"))).contains(
            d("2027-01-02")
        );
        assertThat(policy.next(rule, d("2026-12-31"), d("2027-01-01"), d("2027-01-01"))).isEmpty();
    }

    @Test
    void weeklyUsesSundayWeeksAndMultipleWeekdays() {
        var rule = new RecurrenceRule(RecurrenceRule.Frequency.WEEKLY, 2, Set.of(1, 7), 1);
        assertThat(policy.next(rule, d("2026-09-07"), null, d("2026-09-06"))).contains(
            d("2026-09-07")
        );
        assertThat(policy.next(rule, d("2026-09-07"), null, d("2026-09-07"))).contains(
            d("2026-09-20")
        );
        assertThat(policy.next(rule, d("2026-09-07"), null, d("2026-09-20"))).contains(
            d("2026-09-21")
        );
        assertThat(
            policy.next(
                new RecurrenceRule(RecurrenceRule.Frequency.WEEKLY, 1, Set.of(1), 1),
                d("2026-09-07"),
                null,
                d("2026-09-07")
            )
        ).contains(d("2026-09-14"));
    }

    @Test
    void monthlySkipsInvalidDaysAndHonorsLeapYearsAndIntervals() {
        var rule = new RecurrenceRule(RecurrenceRule.Frequency.MONTHLY, 1, Set.of(), 31);
        assertThat(policy.next(rule, d("2026-01-31"), null, d("2026-01-31"))).contains(
            d("2026-03-31")
        );
        assertThat(policy.next(rule, d("2026-01-31"), d("2026-03-31"), d("2026-01-31"))).contains(
            d("2026-03-31")
        );
        assertThat(policy.next(rule, d("2026-01-31"), d("2026-03-30"), d("2026-01-31"))).isEmpty();
        assertThat(
            policy.next(
                new RecurrenceRule(RecurrenceRule.Frequency.MONTHLY, 12, Set.of(), 29),
                d("2027-02-01"),
                null,
                d("2027-02-01")
            )
        ).contains(d("2028-02-29"));
        assertThat(
            policy.next(
                new RecurrenceRule(RecurrenceRule.Frequency.MONTHLY, 12, Set.of(), 31),
                d("2026-02-01"),
                null,
                d("2026-01-31")
            )
        ).isEmpty();
        assertThat(
            policy.next(
                new RecurrenceRule(RecurrenceRule.Frequency.MONTHLY, 2, Set.of(), 1),
                d("2026-12-01"),
                null,
                d("2026-12-01")
            )
        ).contains(d("2027-02-01"));
    }

    @Test
    void nextOccurrenceDecisionPreservesExistingAndPartial() {
        assertThat(policy.shouldGenerate(false, false, "COMPLETED")).isTrue();
        assertThat(policy.shouldGenerate(false, false, "SKIPPED")).isTrue();
        assertThat(policy.shouldGenerate(true, false, "COMPLETED")).isFalse();
        assertThat(policy.shouldGenerate(false, true, "COMPLETED")).isFalse();
        assertThat(policy.shouldGenerate(false, false, "PARTIAL")).isFalse();
        assertThat(policy.scheduledDate(d("2026-09-06"), d("2026-09-07"))).isEqualTo(
            d("2026-09-07")
        );
        assertThat(policy.scheduledDate(d("2026-09-07"), d("2026-09-07"))).isEqualTo(
            d("2026-09-07")
        );
        assertThat(policy.scheduledDate(d("2026-09-08"), d("2026-09-07"))).isEqualTo(
            d("2026-09-08")
        );
    }
}
