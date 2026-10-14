create table task_reminder (
    task_id uuid primary key,
    user_id uuid not null,
    revision bigint not null check (revision >= 0),
    scheduled_for timestamptz,
    enabled boolean not null,
    check (not enabled or scheduled_for is not null)
);
create index task_reminder_owner_schedule on task_reminder(user_id,scheduled_for) where enabled;

alter table notification_delivery drop constraint notification_delivery_kind_check;
alter table notification_delivery add constraint notification_delivery_kind_check check (kind in ('DAILY_SUMMARY','TASK_REMINDER'));
alter table notification_delivery drop constraint notification_delivery_user_id_kind_scheduled_for_key;
alter table notification_delivery add column task_id uuid, add column task_revision bigint;
alter table notification_delivery add constraint notification_delivery_task_identity check (
    (kind='DAILY_SUMMARY' and task_id is null and task_revision is null) or
    (kind='TASK_REMINDER' and task_id is not null and task_revision is not null)
);
create unique index notification_summary_schedule on notification_delivery(user_id,scheduled_for) where kind='DAILY_SUMMARY';
create unique index notification_task_revision on notification_delivery(task_id,task_revision) where kind='TASK_REMINDER';
