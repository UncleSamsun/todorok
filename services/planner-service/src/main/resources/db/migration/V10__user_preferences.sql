create table user_preference (
    user_id uuid primary key,
    theme varchar(10) not null check (theme in ('SYSTEM','LIGHT','DARK')),
    revision bigint not null check (revision >= 0),
    updated_at timestamptz not null default now()
);
