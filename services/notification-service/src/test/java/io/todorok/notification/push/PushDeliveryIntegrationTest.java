package io.todorok.notification.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PushDeliveryIntegrationTest extends NotificationPushIntegrationTest {
    @MockitoBean PushSender sender;
    @MockitoBean(name="notificationClock") Clock clock;
    @Autowired ApplicationContext context;
    @Autowired PushSubscriptions subscriptions;
    private final Instant now=Instant.parse("2026-10-12T00:00:00Z");
    @BeforeEach void prepare(){when(clock.instant()).thenReturn(now);when(sender.send(any(),any(),any())).thenReturn(201);jdbc.update("update notification_delivery set status='CANCELED' where status='PENDING'");}
    void tick()throws Exception {if(context.containsBean("pushDeliveryWorker"))context.getBean("pushDeliveryWorker").getClass().getMethod("tick").invoke(context.getBean("pushDeliveryWorker"));}
    UUID delivery(UUID owner,Instant deadline){UUID id=UUID.randomUUID();jdbc.update("insert into notification_preference(user_id,notifications_enabled,summary_time,revision) values (?,true,'08:00',0)",owner);jdbc.update("insert into notification_delivery(id,user_id,kind,scheduled_for,deadline_at,status,next_attempt_at) values (?,?,'DAILY_SUMMARY',?,?,'PENDING',?)",id,owner,OffsetDateTime.ofInstant(now.minusSeconds(60),ZoneOffset.UTC),OffsetDateTime.ofInstant(deadline,ZoneOffset.UTC),OffsetDateTime.ofInstant(now.minusSeconds(60),ZoneOffset.UTC));return id;}
    UUID subscribe(UUID owner)throws Exception {var node=mapper.readTree(subscription("https://fcm.googleapis.com/"+UUID.randomUUID()));return subscriptions.save(owner,new PushSecrets.Secret(node.get("endpoint").asText(),node.get("keys").get("p256dh").asText(),node.get("keys").get("auth").asText()));}
    String status(UUID id){return jdbc.queryForObject("select status from notification_delivery where id=?",String.class,id);}
    @Test void sendsOncePerSubscriptionAndSchedulesTomorrow()throws Exception {
        UUID owner=UUID.randomUUID();subscribe(owner);UUID id=delivery(owner,now.plusSeconds(3600));tick();tick();
        assertThat(status(id)).isEqualTo("DELIVERED");verify(sender,times(1)).send(any(),eq(id),any());
        assertThat(jdbc.queryForObject("select count(*) from notification_delivery where user_id=? and status='PENDING' and scheduled_for=?",Integer.class,owner,OffsetDateTime.parse("2026-10-13T08:00:00+09:00"))).isEqualTo(1);
    }
    @Test void retriesOnlyUnsentTargetsAndDiscardsGoneSubscriptions()throws Exception {
        UUID owner=UUID.randomUUID();subscribe(owner);subscribe(owner);UUID id=delivery(owner,now.plusSeconds(3600));
        when(sender.send(any(),eq(id),any())).thenReturn(201,503,410);
        tick();assertThat(status(id)).isEqualTo("PENDING");
        when(clock.instant()).thenReturn(now.plusSeconds(120));tick();
        assertThat(status(id)).isEqualTo("FAILED");verify(sender,times(3)).send(any(),eq(id),any());
        assertThat(jdbc.queryForObject("select count(*) from push_subscription where user_id=? and active",Integer.class,owner)).isEqualTo(1);
    }
    @Test void expiresWithoutSendingAndCancelsDisabledPreference()throws Exception {
        UUID owner=UUID.randomUUID();subscribe(owner);UUID id=delivery(owner,now.minusSeconds(1));tick();
        assertThat(status(id)).isEqualTo("EXPIRED");verify(sender,never()).send(any(),eq(id),any());
        UUID other=UUID.randomUUID();subscribe(other);UUID canceled=delivery(other,now.plusSeconds(3600));
        jdbc.update("update notification_preference set notifications_enabled=false where user_id=?",other);tick();
        assertThat(status(canceled)).isEqualTo("CANCELED");verify(sender,never()).send(any(),eq(canceled),any());
    }
    @Test void recoversExpiredLeasesButLeavesLiveClaimsAlone()throws Exception {
        UUID owner=UUID.randomUUID();subscribe(owner);UUID id=delivery(owner,now.plusSeconds(3600));
        jdbc.update("update notification_delivery set lease_token=?,lease_until=? where id=?",UUID.randomUUID(),OffsetDateTime.ofInstant(now.plusSeconds(30),ZoneOffset.UTC),id);
        tick();assertThat(status(id)).isEqualTo("PENDING");
        when(clock.instant()).thenReturn(now.plusSeconds(31));tick();assertThat(status(id)).isEqualTo("DELIVERED");
    }
    @Test void doesNotCommitAResultAfterLosingItsLease()throws Exception {
        UUID owner=UUID.randomUUID();subscribe(owner);UUID id=delivery(owner,now.plusSeconds(3600));
        when(sender.send(any(),eq(id),any())).thenAnswer(call->{when(clock.instant()).thenReturn(now.plusSeconds(121));return 201;})
            .thenAnswer(call->{
                assertThat(jdbc.queryForObject("select status from notification_attempt where delivery_id=?",String.class,id)).isEqualTo("PENDING");
                return 201;
            });
        tick();
        assertThat(status(id)).isEqualTo("DELIVERED");
        verify(sender,times(2)).send(any(),eq(id),any());
    }
    @Test void expiresWhenDeadlinePassesDuringSend()throws Exception {
        UUID owner=UUID.randomUUID();subscribe(owner);UUID id=delivery(owner,now.plusSeconds(10));
        when(sender.send(any(),eq(id),any())).thenAnswer(call->{when(clock.instant()).thenReturn(now.plusSeconds(11));return 201;});
        tick();assertThat(status(id)).isEqualTo("EXPIRED");
    }
}
