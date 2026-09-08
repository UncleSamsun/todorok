create table notification_preference (
    user_id uuid primary key,
    notifications_enabled boolean not null,
    summary_time char(5) not null check (summary_time ~ '^(?:[01][0-9]|2[0-3]):[0-5][0-9]$'),
    revision bigint not null check (revision >= 0),
    updated_at timestamptz not null default now()
);
