package io.todorok.notification.push;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class TaskReminderIntegrationTest extends NotificationPushIntegrationTest {
    @Autowired ApplicationContext context;
    @MockitoBean(name="notificationClock") Clock clock;
    @MockitoBean PushSender sender;
    final Instant now=Instant.parse("2026-10-12T00:00:00Z");
    @BeforeEach void prepare(){when(clock.instant()).thenReturn(now);when(sender.send(any(),any(),any())).thenReturn(201);jdbc.update("update notification_delivery set status='CANCELED' where status='PENDING'");}
    void receive(UUID owner,UUID task,long revision,String scheduled,boolean enabled)throws Exception {
        assertThat(context.containsBean("taskReminderConsumer")).as("Task reminder consumer must be installed").isTrue();
        var payload=new LinkedHashMap<String,Object>();payload.put("taskId",task);payload.put("scheduledFor",scheduled);payload.put("enabled",enabled);
        String event=mapper.writeValueAsString(Map.of("eventId",UUID.randomUUID(),"type","TASK_REMINDER_CHANGED","version",1,
            "aggregateVersion",revision,"occurredAt",now,"userId",owner,"payload",payload));
        context.getBean("taskReminderConsumer").getClass().getMethod("receive",String.class).invoke(context.getBean("taskReminderConsumer"),event);
    }
    void tick()throws Exception {var worker=context.getBean("pushDeliveryWorker");worker.getClass().getMethod("tick").invoke(worker);}
    UUID enable(){UUID owner=UUID.randomUUID();jdbc.update("insert into notification_preference(user_id,notifications_enabled,summary_time,revision) values (?,true,'08:00',0)",owner);return owner;}
    String state(UUID task){return jdbc.queryForObject("select status from notification_delivery where task_id=? order by created_at desc,id limit 1",String.class,task);}
    @Test void distinctTasksAtTheSameTimeHaveIndependentDeadlinesAndDuplicateEventsDoNotRecreate()throws Exception {
        UUID owner=enable(),a=UUID.randomUUID(),b=UUID.randomUUID();
        receive(owner,a,0,"2026-10-12T10:00:00+09:00",true);receive(owner,a,0,"2026-10-12T10:00:00+09:00",true);receive(owner,b,0,"2026-10-12T10:00:00+09:00",true);
        assertThat(jdbc.queryForObject("select count(*) from notification_delivery where user_id=? and kind='TASK_REMINDER'",Integer.class,owner)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select deadline_at from notification_delivery where task_id=?",OffsetDateTime.class,a).toInstant()).isEqualTo(Instant.parse("2026-10-12T01:30:00Z"));
        tick();assertThat(jdbc.queryForObject("select count(*) from notification_delivery where user_id=? and kind='DAILY_SUMMARY' and status='PENDING'",Integer.class,owner)).isEqualTo(1);
    }
    @Test void changesAndCompletionCancelOldReservationsAndIgnoreOlderEvents()throws Exception {
        UUID owner=enable(),task=UUID.randomUUID();receive(owner,task,0,"2026-10-12T10:00:00+09:00",true);
        receive(owner,task,1,"2026-10-12T11:00:00+09:00",true);
        assertThat(jdbc.queryForObject("select count(*) from notification_delivery where task_id=? and status='CANCELED'",Integer.class,task)).isOne();
        receive(owner,task,3,null,false);receive(owner,task,2,"2026-10-12T12:00:00+09:00",true);
        assertThat(jdbc.queryForObject("select count(*) from notification_delivery where task_id=? and status='PENDING'",Integer.class,task)).isZero();
    }
    @Test void quietHoursAndElapsedDeadlinesExpireInsteadOfDeferring()throws Exception {
        UUID owner=enable();
        for(String time:List.of("2026-10-12T22:00:00+09:00","2026-10-13T06:59:00+09:00","2026-10-12T08:29:00+09:00")){
            UUID task=UUID.randomUUID();receive(owner,task,0,time,true);assertThat(state(task)).isEqualTo("EXPIRED");
        }
        UUID boundary=UUID.randomUUID();receive(owner,boundary,0,"2026-10-13T07:00:00+09:00",true);assertThat(state(boundary)).isEqualTo("PENDING");
        verifyNoInteractions(sender);
    }
    @Test void aRetryCrossingIntoQuietHoursDoesNotSend()throws Exception {
        UUID owner=enable(),task=UUID.randomUUID();
        var node=mapper.readTree(subscription("https://fcm.googleapis.com/"+UUID.randomUUID()));
        context.getBean(PushSubscriptions.class).save(owner,new PushSecrets.Secret(node.get("endpoint").asText(),node.get("keys").get("p256dh").asText(),node.get("keys").get("auth").asText()));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-12T12:59:45Z"));
        receive(owner,task,0,"2026-10-12T21:59:00+09:00",true);when(sender.send(any(),any(),any())).thenReturn(503);tick();
        assertThat(state(task)).isEqualTo("PENDING");when(clock.instant()).thenReturn(Instant.parse("2026-10-12T13:00:16Z"));tick();
        assertThat(state(task)).isEqualTo("EXPIRED");verify(sender,times(1)).send(any(),any(),any());
    }
    @Test void reEnablingNotificationsRestoresOnlyFutureExplicitReminders()throws Exception {
        UUID owner=enable(),task=UUID.randomUUID();receive(owner,task,0,"2026-10-12T10:00:00+09:00",true);
        var schedules=context.getBean(io.todorok.notification.preference.NotificationScheduleService.class);
        jdbc.update("update notification_preference set notifications_enabled=false where user_id=?",owner);schedules.apply(owner,false,"08:00");
        assertThat(state(task)).isEqualTo("CANCELED");
        receive(owner,task,1,"2026-10-12T10:00:00+09:00",true); // A title-only change must not duplicate it.
        jdbc.update("update notification_preference set notifications_enabled=true where user_id=?",owner);schedules.apply(owner,true,"08:00");
        assertThat(state(task)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("select count(*) from notification_delivery where task_id=?",Integer.class,task)).isOne();
        jdbc.update("update notification_preference set notifications_enabled=false where user_id=?",owner);schedules.apply(owner,false,"08:00");
        receive(owner,task,2,"2026-10-12T11:00:00+09:00",true);
        receive(owner,task,3,"2026-10-12T10:00:00+09:00",true);
        jdbc.update("update notification_preference set notifications_enabled=true where user_id=?",owner);schedules.apply(owner,true,"08:00");
        assertThat(jdbc.queryForObject("select count(*) from notification_delivery where task_id=? and status='PENDING'",Integer.class,task)).isOne();
    }
    @Test void temporaryFailuresDoNotExpireTheReminderBeforeItsThirtyMinuteDeadline()throws Exception {
        UUID owner=enable(),task=UUID.randomUUID();
        var node=mapper.readTree(subscription("https://fcm.googleapis.com/"+UUID.randomUUID()));
        context.getBean(PushSubscriptions.class).save(owner,new PushSecrets.Secret(node.get("endpoint").asText(),node.get("keys").get("p256dh").asText(),node.get("keys").get("auth").asText()));
        receive(owner,task,0,"2026-10-12T09:00:00+09:00",true);when(sender.send(any(),any(),any())).thenReturn(503);
        for(int seconds:new int[]{0,30,90,210,450,930}){when(clock.instant()).thenReturn(now.plusSeconds(seconds));tick();}
        assertThat(state(task)).isEqualTo("PENDING");
        var retry=jdbc.queryForObject("select next_attempt_at from notification_delivery where task_id=?",OffsetDateTime.class,task).toInstant();
        assertThat(retry).isBefore(now.plusSeconds(1800));
        when(clock.instant()).thenReturn(retry);when(sender.send(any(),any(),any())).thenReturn(201);tick();assertThat(state(task)).isEqualTo("DELIVERED");
    }
    @Test void reEnablingAndTaskCompletionSerializeBeforeRestoringTheReservation()throws Exception {
        UUID owner=enable(),task=UUID.randomUUID();receive(owner,task,0,"2026-10-12T10:00:00+09:00",true);
        var schedules=context.getBean(io.todorok.notification.preference.NotificationScheduleService.class);
        schedules.apply(owner,false,"08:00");
        try(var connection=jdbc.getDataSource().getConnection();var pool=java.util.concurrent.Executors.newSingleThreadExecutor()){
            connection.setAutoCommit(false);
            try(var lock=connection.prepareStatement("select pg_advisory_xact_lock(hashtextextended(?,0))")){
                lock.setString(1,"task-reminder:"+task);lock.execute();
            }
            try(var completed=connection.prepareStatement("update task_reminder set enabled=false,revision=1 where task_id=?")){
                completed.setObject(1,task);completed.executeUpdate();
            }
            var restored=pool.submit(()->schedules.apply(owner,true,"08:00"));
            try {
                assertThatThrownBy(()->restored.get(2,java.util.concurrent.TimeUnit.SECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);
            } finally { connection.commit(); }
            restored.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("select count(*) from notification_delivery where task_id=? and status='PENDING'",Integer.class,task)).isZero();
        }
    }
    @Test void summaryReservationsRecoverWhenReEnabledOrChangedBackToTheOriginalTime(){
        UUID owner=enable();
        var schedules=context.getBean(io.todorok.notification.preference.NotificationScheduleService.class);
        schedules.apply(owner,true,"09:30");schedules.apply(owner,false,"09:30");schedules.apply(owner,true,"09:30");
        assertThat(jdbc.queryForObject("select count(*) from notification_delivery where user_id=? and kind='DAILY_SUMMARY' and status='PENDING'",Integer.class,owner)).isOne();
        schedules.apply(owner,true,"09:45");schedules.apply(owner,true,"09:30");
        assertThat(jdbc.queryForObject("select count(*) from notification_delivery where user_id=? and kind='DAILY_SUMMARY' and status='PENDING'",Integer.class,owner)).isOne();
        assertThat(jdbc.queryForObject("select scheduled_for from notification_delivery where user_id=? and kind='DAILY_SUMMARY' and status='PENDING'",OffsetDateTime.class,owner).toInstant()).isEqualTo(now.plusSeconds(1800));
    }
}
