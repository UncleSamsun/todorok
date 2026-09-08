package io.todorok.notification.preference;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class NotificationScheduleServiceTest {
    @Test void createsOneFutureSummaryAndCancelsItWhenNotificationsAreDisabled() {
        var jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        var service = new NotificationScheduleService(jdbc, Clock.fixed(Instant.parse("2026-09-08T00:30:00Z"), ZoneOffset.UTC));
        UUID owner = UUID.randomUUID();

        service.apply(owner, true, "08:00");
        service.apply(owner, false, "08:00");

        var values = org.mockito.ArgumentCaptor.forClass(Object[].class);
        org.mockito.Mockito.verify(jdbc).update(org.mockito.ArgumentMatchers.contains("insert into notification_delivery"), values.capture());
        assertThat(values.getValue()[2]).isEqualTo(OffsetDateTime.parse("2026-09-09T08:00:00+09:00"));
        org.mockito.Mockito.verify(jdbc).update(org.mockito.ArgumentMatchers.contains("status='CANCELED'"), org.mockito.ArgumentMatchers.eq(owner));
    }
}
