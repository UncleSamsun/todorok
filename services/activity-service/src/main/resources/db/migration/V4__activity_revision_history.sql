alter table activity_record add column previous_performed_at timestamptz;
create table activity_revision_history (
    activity_id uuid not null references activity_record(id),
    revision bigint not null,
    snapshot jsonb not null,
    replaced_at timestamptz not null default now(),
    primary key(activity_id, revision)
);
create function reject_activity_history_mutation() returns trigger language plpgsql as $$
begin
    raise exception 'Activity revision history is immutable';
end $$;
create trigger immutable_activity_revision_history before update or delete on activity_revision_history
for each row execute function reject_activity_history_mutation();
