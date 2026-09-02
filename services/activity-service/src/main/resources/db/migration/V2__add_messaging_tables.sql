create table outbox_event (
    id uuid primary key,
    aggregatetype varchar(80) not null,
    aggregateid varchar(255) not null,
    type varchar(100) not null,
    payload jsonb not null,
    occurred_at timestamptz not null,
    created_at timestamptz not null default now(),
    constraint outbox_payload_is_object
        check (jsonb_typeof(payload) = 'object')
);

create index outbox_event_created_at_idx on outbox_event(created_at);

create table processed_event (
    event_id uuid primary key,
    event_type varchar(100) not null,
    processed_at timestamptz not null default now()
);

create index processed_event_processed_at_idx on processed_event(processed_at);
