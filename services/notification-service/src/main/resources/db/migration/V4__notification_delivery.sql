create table notification_delivery (
    id uuid primary key,
    user_id uuid not null,
    kind varchar(20) not null check (kind in ('DAILY_SUMMARY')),
    scheduled_for timestamptz not null,
    deadline_at timestamptz not null check (deadline_at > scheduled_for),
    status varchar(20) not null check (status in ('PENDING','CANCELED','EXPIRED','DELIVERED','FAILED')),
    attempts integer not null default 0 check (attempts >= 0),
    created_at timestamptz not null default now(),
    unique (user_id, kind, scheduled_for)
);
