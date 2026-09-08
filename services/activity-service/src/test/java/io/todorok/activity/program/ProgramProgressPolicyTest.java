package io.todorok.activity.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProgramProgressPolicyTest {
    private final ProgramProgressPolicy policy = new ProgramProgressPolicy();
    private static ProgramProgressPolicy.Attempt attempt(long sequence, ProgramProgressPolicy.Outcome outcome) {
        return new ProgramProgressPolicy.Attempt(sequence, outcome);
    }

    @Test void advancesOnlyAfterEverySessionInTheCycleSucceeds() {
        assertThat(policy.calculate(1, 2, 3, List.of(attempt(2, ProgramProgressPolicy.Outcome.SUCCESS), attempt(1, ProgramProgressPolicy.Outcome.SUCCESS))))
            .isEqualTo(new ProgramProgressPolicy.Progress(1, 3, false));
        assertThat(policy.calculate(1, 2, 3, List.of(attempt(3, ProgramProgressPolicy.Outcome.SUCCESS), attempt(1, ProgramProgressPolicy.Outcome.SUCCESS), attempt(2, ProgramProgressPolicy.Outcome.SUCCESS))))
            .isEqualTo(new ProgramProgressPolicy.Progress(2, 1, false));
    }

    @Test void repeatsTheWeekAfterAnyFailureAndCanAdvanceOnTheNextCycle() {
        var history = List.of(
            attempt(1, ProgramProgressPolicy.Outcome.SUCCESS), attempt(2, ProgramProgressPolicy.Outcome.FAILURE), attempt(3, ProgramProgressPolicy.Outcome.SUCCESS),
            attempt(4, ProgramProgressPolicy.Outcome.SUCCESS), attempt(5, ProgramProgressPolicy.Outcome.SUCCESS), attempt(6, ProgramProgressPolicy.Outcome.SUCCESS));
        assertThat(policy.calculate(1, 3, 3, history)).isEqualTo(new ProgramProgressPolicy.Progress(2, 1, false));
    }

    @Test void ignoresVoidedAttemptsSoCorrectionHistoryCanBeRecomputed() {
        var history = List.of(attempt(1, ProgramProgressPolicy.Outcome.SUCCESS), attempt(2, ProgramProgressPolicy.Outcome.SUCCESS),
            attempt(3, ProgramProgressPolicy.Outcome.VOIDED));
        assertThat(policy.calculate(1, 2, 3, history)).isEqualTo(new ProgramProgressPolicy.Progress(1, 3, false));
    }

    @Test void completesAtTheEndAndRejectsInvalidBounds() {
        assertThat(policy.calculate(2, 2, 2, List.of(attempt(1, ProgramProgressPolicy.Outcome.SUCCESS), attempt(2, ProgramProgressPolicy.Outcome.SUCCESS))))
            .isEqualTo(new ProgramProgressPolicy.Progress(2, 2, true));
        assertThatThrownBy(() -> policy.calculate(0, 2, 3, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.calculate(2, 1, 3, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.calculate(1, 2, 0, List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
