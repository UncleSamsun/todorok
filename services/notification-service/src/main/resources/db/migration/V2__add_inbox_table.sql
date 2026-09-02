create table processed_event (
    event_id uuid primary key,
    event_type varchar(100) not null,
    processed_at timestamptz not null default now()
);

create index processed_event_processed_at_idx on processed_event(processed_at);
