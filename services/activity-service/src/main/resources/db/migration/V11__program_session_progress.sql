alter table program_enrollment add column current_cycle integer not null default 1 check (current_cycle >= 1);
