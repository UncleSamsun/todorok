do $$
declare
    slot_active boolean;
    retained_wal_bytes numeric;
begin
    select active,
           pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn)
      into slot_active, retained_wal_bytes
      from pg_replication_slots
     where slot_name = 'todorok_outbox_slot';

    if not found then
        raise exception 'todorok_outbox_slot does not exist';
    end if;
    if not slot_active then
        raise exception 'todorok_outbox_slot is not active';
    end if;
    if retained_wal_bytes is null or retained_wal_bytes > 16777216 then
        raise exception 'replication slot WAL lag is unsafe: %', retained_wal_bytes;
    end if;

    delete from planner.outbox_event
     where created_at < now() - interval '7 days';
    delete from activity.outbox_event
     where created_at < now() - interval '7 days';
    delete from planner.processed_event
     where processed_at < now() - interval '30 days';
    delete from activity.processed_event
     where processed_at < now() - interval '30 days';
    delete from notification.processed_event
     where processed_at < now() - interval '30 days';
end
$$;
