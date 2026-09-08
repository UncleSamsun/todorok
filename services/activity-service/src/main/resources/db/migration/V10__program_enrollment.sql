create table program_enrollment (
    id uuid primary key,
    user_id uuid not null,
    catalog_key varchar(120) not null,
    catalog_version bigint not null,
    command_id uuid not null,
    request_fingerprint char(64) not null,
    initial_test_value integer not null check (initial_test_value >= 0),
    recommended_week integer not null check (recommended_week >= 1),
    start_week integer not null check (start_week >= 1),
    current_week integer not null check (current_week >= 1),
    current_session integer not null check (current_session >= 1),
    status varchar(16) not null check (status in ('ACTIVE','COMPLETED')),
    revision bigint not null default 0,
    created_at timestamptz not null default now(),
    unique (user_id, command_id),
    foreign key (catalog_key, catalog_version) references program_catalog(catalog_key, catalog_version)
);

create table program_session (
    id uuid primary key,
    enrollment_id uuid not null references program_enrollment(id),
    cycle integer not null check (cycle >= 1),
    week integer not null check (week >= 1),
    session integer not null check (session >= 1),
    target_sets jsonb not null check (jsonb_typeof(target_sets)='array'),
    task_id uuid unique,
    activity_id uuid unique,
    outcome varchar(16) check (outcome in ('SUCCESS','FAILURE','VOIDED')),
    created_at timestamptz not null default now(),
    unique (enrollment_id, cycle, session)
);
