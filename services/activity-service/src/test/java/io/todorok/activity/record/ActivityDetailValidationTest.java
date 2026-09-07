package io.todorok.activity.record;

import static org.assertj.core.api.Assertions.*;
import io.todorok.activity.api.model.*;
import io.todorok.web.ApiFailure;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityDetailValidationTest {
    private final ActivityDetailStore details = new ActivityDetailStore(null, null);

    @Test
    void acceptsValidImmutableSetsAndRounds() {
        assertThatCode(() -> details.validate(ActivityType.WORKOUT, new ActivityDetail()
            .workout(new WorkoutDetail().sets(List.of(new WorkoutSet().reps(5)))))).doesNotThrowAnyException();
        assertThatCode(() -> details.validate(ActivityType.CLIMBING, new ActivityDetail()
            .climbing(new ClimbingDetail().rounds(List.of(new ClimbingRound().attempts(2)))))).doesNotThrowAnyException();
    }

    @Test
    void rejectsNullSetWithTypedValidationFailure() {
        assertThatThrownBy(() -> details.validate(ActivityType.WORKOUT, new ActivityDetail()
            .workout(new WorkoutDetail().sets(Collections.singletonList(null)))))
            .isInstanceOf(ApiFailure.class);
    }

    @Test
    void rejectsNullRoundWithTypedValidationFailure() {
        assertThatThrownBy(() -> details.validate(ActivityType.CLIMBING, new ActivityDetail()
            .climbing(new ClimbingDetail().rounds(Collections.singletonList(null)))))
            .isInstanceOf(ApiFailure.class);
    }
}
