alter table task add column activity_id uuid;
alter table task add column performed_at timestamptz;
alter table task add column started_at timestamptz;
alter table task add column ended_at timestamptz;
alter table task add column completion_summary text;
create unique index task_activity_unique on task(activity_id) where activity_id is not null;
create table activity_completion_result (
    activity_id uuid primary key, task_id uuid not null, user_id uuid not null,
    revision bigint not null check(revision>=0), state varchar(20) not null,
    reason varchar(100) not null
);
