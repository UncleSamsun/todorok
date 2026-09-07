create table task_reference (
    task_id uuid primary key, user_id uuid not null,
    task_type varchar(20) not null check (task_type in ('GENERAL','WORKOUT','STUDY','CLIMBING')),
    scheduled_date date not null,
    status varchar(20) not null check (status in ('PLANNED','COMPLETED','SKIPPED','DELETED')),
    version bigint not null check (version >= 0)
);
create table activity_record (
    id uuid primary key, user_id uuid not null, task_id uuid not null,
    activity_type varchar(20) not null check (activity_type in ('WORKOUT','STUDY','CLIMBING')),
    performed_at timestamptz not null, started_at timestamptz, ended_at timestamptz,
    created_at timestamptz not null default now(),
    status varchar(20) not null check (status in ('COMPLETED','PARTIAL','VOIDED')),
    command_id uuid not null, fingerprint varchar(64) not null,
    revision bigint not null default 0 check (revision >= 0), note text,
    sync_state varchar(20) not null check (sync_state in ('PENDING','APPLIED','CONFLICT','NOT_REQUIRED')),
    sync_reason varchar(100), void_reason varchar(500),
    unique(user_id, command_id),
    check ((started_at is null and ended_at is null) or (started_at is not null and ended_at is not null and ended_at>started_at))
);
create unique index one_completed_activity_per_task on activity_record(task_id) where status='COMPLETED';
create index activity_owner_date on activity_record(user_id, performed_at, id);
create table workout_detail (activity_id uuid primary key references activity_record(id));
create table workout_set (
    activity_id uuid not null references workout_detail(activity_id), position integer not null,
    exercise varchar(120), reps integer check(reps>=0), weight_kg numeric check(weight_kg>=0),
    duration_seconds integer check(duration_seconds>=0), primary key(activity_id, position)
);
create table study_detail (
    activity_id uuid primary key references activity_record(id), subject varchar(120),
    duration_minutes integer check(duration_minutes>=0), values_json jsonb, snapshot jsonb
);
create table climbing_detail (activity_id uuid primary key references activity_record(id), duration_seconds integer check(duration_seconds>=0));
create table climbing_round (
    activity_id uuid not null references climbing_detail(activity_id), position integer not null,
    grade varchar(40), attempts integer check(attempts>=0), completed boolean,
    primary key(activity_id, position)
);
