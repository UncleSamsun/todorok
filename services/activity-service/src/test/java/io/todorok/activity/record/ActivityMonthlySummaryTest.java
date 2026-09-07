package io.todorok.activity.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.todorok.activity.api.model.ActivityType;
import io.todorok.messaging.OutboxEventWriter;
import io.todorok.web.ApiFailure;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

class ActivityMonthlySummaryTest {
    @Test
    void usesTheSeoulMonthAtTheUtcBoundaryAndRejectsOnlyLaterMonths() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForMap(any(String.class), any(), any(), any(), any()))
            .thenReturn(Map.of("completed_count", 0, "duration_seconds", 0L));
        var service = new ActivityService(jdbc, mock(ActivityDetailStore.class),
            mock(OutboxEventWriter.class), mock(ObjectMapper.class),
            Clock.fixed(Instant.parse("2026-08-31T15:00:00Z"), ZoneOffset.UTC));

        assertThat(service.monthlySummary(UUID.randomUUID(), YearMonth.of(2026, 9), ActivityType.STUDY).getMonth())
            .isEqualTo("2026-09");
        assertThatThrownBy(() -> service.monthlySummary(UUID.randomUUID(), YearMonth.of(2026, 10), ActivityType.STUDY))
            .isInstanceOf(ApiFailure.class)
            .extracting("code").isEqualTo("FUTURE_MONTH");
    }
}
