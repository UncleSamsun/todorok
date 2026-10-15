alter table program_enrollment add column start_session integer not null default 1 check (start_session between 1 and 7);
alter table program_enrollment add column progress_epoch bigint not null default 1 check (progress_epoch >= 1);
alter table program_enrollment add column epoch_start_week integer;
update program_enrollment set epoch_start_week=start_week;
alter table program_enrollment alter column epoch_start_week set not null;
alter table program_enrollment alter column epoch_start_week set default 1;
alter table program_enrollment add constraint program_epoch_week_positive check (epoch_start_week >= 1);
alter table program_enrollment add column epoch_start_session integer not null default 1 check (epoch_start_session between 1 and 7);
alter table program_enrollment add column last_progress_at timestamptz;
update program_enrollment set last_progress_at=created_at;
alter table program_enrollment alter column last_progress_at set not null;
alter table program_enrollment alter column last_progress_at set default now();
alter table program_session add column progress_epoch bigint not null default 1 check (progress_epoch >= 1);
create table program_schedule_command (
    user_id uuid not null,
    command_id uuid not null,
    enrollment_id uuid not null references program_enrollment(id),
    fingerprint char(64) not null,
    result jsonb not null,
    created_at timestamptz not null default now(),
    primary key(user_id,command_id)
);
