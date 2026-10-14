package io.todorok.notification.push;

import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PushDeliveryWorker {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final PushSecrets secrets;
    private final PushSender sender;
    private final Clock clock;
    private final Environment env;
    public PushDeliveryWorker(JdbcTemplate jdbc,PlatformTransactionManager manager,PushSecrets secrets,PushSender sender,Clock clock,Environment env){
        this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);this.secrets=secrets;this.sender=sender;this.clock=clock;this.env=env;
    }
    @Configuration @EnableScheduling static class Scheduling {}
    @Scheduled(fixedDelayString="${todorok.push.poll-interval-ms:15000}")
    public void scheduledTick(){
        if(!env.getProperty("todorok.push.worker-enabled",Boolean.class,false))return;
        try{secrets.requireEnabled();tick();}
        catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Push worker cycle failed; pending deliveries remain recoverable.");}
    }
    private OffsetDateTime time(Instant instant){return instant.atOffset(ZoneOffset.UTC);}
    public void tick(){
        Instant now=clock.instant();
        jdbc.update("update notification_delivery set status='EXPIRED',error_code='DEADLINE_EXCEEDED',lease_token=null,lease_until=null where status='PENDING' and deadline_at<=?",time(now));
        expireQuietReminders(now);
        for(int count=0;count<20;count++){
            Job job=claim();if(job==null)break;
            deliver(job);
        }
        ensureNextSummaries();
    }
    private void expireQuietReminders(Instant now){
        var local=now.atZone(SEOUL).toLocalTime();
        if(local.isBefore(LocalTime.of(7,0))||!local.isBefore(LocalTime.of(22,0)))
            jdbc.update("update notification_delivery set status='EXPIRED',error_code='QUIET_HOURS',lease_token=null,lease_until=null where kind='TASK_REMINDER' and status='PENDING' and scheduled_for<=?",time(now));
    }
    private Job claim(){
        return tx.execute(s->{
            Instant now=clock.instant();UUID token=UUID.randomUUID();
            var jobs=jdbc.query("""
                update notification_delivery set lease_token=?,lease_until=?,attempts=attempts+1
                where id=(select id from notification_delivery where status='PENDING' and scheduled_for<=?
                    and next_attempt_at<=? and deadline_at>? and (lease_until is null or lease_until<=?)
                    order by scheduled_for,id for update skip locked limit 1)
                returning id,user_id,deadline_at,attempts
                """,(r,i)->new Job(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,OffsetDateTime.class).toInstant(),token,r.getInt(4)),
                token,time(now.plusSeconds(120)),time(now),time(now),time(now),time(now));
            if(jobs.isEmpty())return null;
            Job job=jobs.getFirst();
            if(!jdbc.queryForObject("select targets_created from notification_delivery where id=?",Boolean.class,job.id())){
                jdbc.update("insert into notification_attempt(delivery_id,subscription_id,status) select ?,id,'PENDING' from push_subscription where user_id=? and active on conflict do nothing",job.id(),job.owner());
                jdbc.update("update notification_delivery set targets_created=true where id=?",job.id());
            }
            return job;
        });
    }
    private boolean live(Job job){
        expireQuietReminders(clock.instant());
        return jdbc.queryForObject("""
            select exists(select 1 from notification_delivery d join notification_preference p on p.user_id=d.user_id
                where d.id=? and d.lease_token=? and d.lease_until>? and d.status='PENDING'
                and d.deadline_at>? and p.notifications_enabled)
            """,Boolean.class,job.id(),job.token(),time(clock.instant()),time(clock.instant()));
    }
    private void deliver(Job job){
        if(!live(job)){invalidate(job);return;}
        var targets=jdbc.query("""
            select s.id,s.endpoint_hash,s.secret,s.active from notification_attempt a
            join push_subscription s on s.id=a.subscription_id where a.delivery_id=? and a.status='PENDING' order by s.id
            """,(r,i)->new Target(r.getObject(1,UUID.class),r.getString(2).trim(),r.getString(3),r.getBoolean(4)),job.id());
        for(Target target:targets){
            if(!live(job))break;
            int status=410;
            boolean active=jdbc.queryForObject("select active from push_subscription where id=?",Boolean.class,target.id());
            if(active && target.secret()!=null){
                try{status=sender.send(secrets.decrypt(job.owner(),target.hash(),target.secret()),job.id(),job.deadline());}
                catch(RuntimeException e){status=0;}
            }
            int result=status;
            tx.executeWithoutResult(s->{
                if(!live(job))return;
                boolean ok=result>=200&&result<300;
                boolean retry=result==0||result==408||result==429||result>=500;
                String state=ok?"DELIVERED":retry?"PENDING":"FAILED";
                String error=ok?null:result==404||result==410?"SUBSCRIPTION_GONE":retry?"PROVIDER_UNAVAILABLE":"PROVIDER_REJECTED";
                jdbc.update("update notification_attempt set status=?,attempts=attempts+1,error_code=? where delivery_id=? and subscription_id=?",state,error,job.id(),target.id());
                if(result==404||result==410)jdbc.update("update push_subscription set active=false,secret=null,updated_at=now() where id=? and secret=?",target.id(),target.secret());
            });
        }
        tx.executeWithoutResult(s->{
            if(!live(job)){invalidate(job);return;}
            int pending=jdbc.queryForObject("select count(*) from notification_attempt where delivery_id=? and status='PENDING'",Integer.class,job.id());
            int total=jdbc.queryForObject("select count(*) from notification_attempt where delivery_id=?",Integer.class,job.id());
            int failed=jdbc.queryForObject("select count(*) from notification_attempt where delivery_id=? and status='FAILED'",Integer.class,job.id());
            if(pending==0){finish(job,total>0&&failed==0?"DELIVERED":"FAILED",total==0?"NO_SUBSCRIPTION":failed>0?"PROVIDER_REJECTED":null);return;}
            Instant now=clock.instant();
            if(!now.isBefore(job.deadline())){finish(job,"EXPIRED","DEADLINE_EXCEEDED");return;}
            Instant retry=now.plusSeconds(Math.min(900,30L << Math.min(job.attempts()-1,5)));
            if(!retry.isBefore(job.deadline()))retry=now.plus(Duration.between(now,job.deadline()).dividedBy(2));
            jdbc.update("update notification_delivery set lease_token=null,lease_until=null,next_attempt_at=?,error_code='PROVIDER_UNAVAILABLE' where id=? and lease_token=? and status='PENDING'",time(retry),job.id(),job.token());
        });
    }
    private void finish(Job job,String status,String error){
        jdbc.update("update notification_delivery set status=?,error_code=?,lease_token=null,lease_until=null where id=? and lease_token=? and lease_until>? and status='PENDING'",status,error,job.id(),job.token(),time(clock.instant()));
    }
    private void invalidate(Job job){
        var now=time(clock.instant());
        jdbc.update("""
            update notification_delivery d set
                status=case when deadline_at<=? then 'EXPIRED' else 'CANCELED' end,
                error_code=case when deadline_at<=? then 'DEADLINE_EXCEEDED' else 'PREFERENCE_DISABLED' end,
                lease_token=null,lease_until=null
            where id=? and lease_token=? and lease_until>? and status='PENDING'
                and (deadline_at<=? or not exists(select 1 from notification_preference p where p.user_id=d.user_id and p.notifications_enabled))
            """,now,now,job.id(),job.token(),now,now);
    }
    private void ensureNextSummaries(){
        tx.executeWithoutResult(s->{
            Instant now=clock.instant();
            var preferences=jdbc.query("""
                select p.user_id,p.summary_time from notification_preference p where p.notifications_enabled
                and not exists(select 1 from notification_delivery d where d.user_id=p.user_id and d.kind='DAILY_SUMMARY' and d.status='PENDING' and d.scheduled_for>?)
                order by p.user_id limit 100 for update skip locked
                """,(r,i)->new Preference(r.getObject(1,UUID.class),r.getString(2).trim()),time(now));
            for(var p:preferences){
                var date=now.atZone(SEOUL).toLocalDate();
                var scheduled=date.atTime(LocalTime.parse(p.time())).atZone(SEOUL);
                if(!scheduled.toInstant().isAfter(now)||!date.atTime(10,0).atZone(SEOUL).toInstant().isAfter(now))scheduled=scheduled.plusDays(1);
                var deadline=scheduled.toLocalDate().atTime(10,0).atZone(SEOUL);
                jdbc.update("insert into notification_delivery(id,user_id,kind,scheduled_for,deadline_at,status) values (?,?,'DAILY_SUMMARY',?,?,'PENDING') on conflict(user_id,scheduled_for) where kind='DAILY_SUMMARY' do nothing",UUID.randomUUID(),p.owner(),scheduled.toOffsetDateTime(),deadline.toOffsetDateTime());
            }
        });
    }
    private record Job(UUID id,UUID owner,Instant deadline,UUID token,int attempts){}
    private record Target(UUID id,String hash,String secret,boolean active){}
    private record Preference(UUID owner,String time){}
}
