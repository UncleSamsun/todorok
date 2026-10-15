package io.todorok.activity.program;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProgramSchedulePolicyTest {
    private final LocalDate performed = LocalDate.of(2026, 10, 15);
    private final ProgramSchedulePolicy policy = new ProgramSchedulePolicy();

    @Test void builtinThreeSessionProgramsIncludeRestDaysAndLongerWeekBreak() {
        for (String key : new String[]{"hundred-pushups-level-1", "hundred-pushups-level-2", "hundred-pushups-level-3", "recon-ron-pullups"}) {
            assertThat(policy.nextDate(key, 3, 1, 2, performed, performed)).isEqualTo(performed.plusDays(2));
            assertThat(policy.nextDate(key, 3, 2, 3, performed, performed)).isEqualTo(performed.plusDays(2));
            assertThat(policy.nextDate(key, 3, 3, 1, performed, performed)).isEqualTo(performed.plusDays(3));
        }
    }

    @Test void usesActualPerformanceDateAndDoesNotScheduleInThePast() {
        assertThat(policy.nextDate("hundred-pushups-level-1", 3, 1, 2, performed, performed.plusDays(1))).isEqualTo(performed.plusDays(2));
        assertThat(policy.nextDate("hundred-pushups-level-1", 3, 1, 2, performed, performed.plusDays(10))).isEqualTo(performed.plusDays(10));
    }

    @Test void preservesLegacyDailyCadenceForOtherCatalogs() {
        assertThat(policy.nextDate("synthetic-pushup", 3, 1, 2, performed, performed)).isEqualTo(performed.plusDays(1));
    }
}
