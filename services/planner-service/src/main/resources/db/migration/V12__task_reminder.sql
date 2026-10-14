alter table task
    add column scheduled_time varchar(5),
    add column reminder_enabled boolean not null default false;
alter table task add constraint task_scheduled_time_format
    check (scheduled_time is null or scheduled_time ~ '^([01][0-9]|2[0-3]):[0-5][0-9]$');
alter table task add constraint task_reminder_requires_time check (not reminder_enabled or scheduled_time is not null);
