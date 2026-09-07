#!/usr/bin/env bash
set -euo pipefail

export PGPASSWORD="$POSTGRES_PASSWORD"
psql_base=(psql --host postgres --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --no-psqlrc)

publication_tables="$(${psql_base[@]} --tuples-only --no-align --command "
select coalesce(string_agg(schemaname || '.' || tablename, ',' order by schemaname, tablename), '')
from pg_publication_tables
where pubname = 'todorok_outbox';
")"

expected_tables="activity.outbox_event,planner.outbox_event"
if [[ -z "$publication_tables" ]]; then
  "${psql_base[@]}" --set=ON_ERROR_STOP=1 --command \
    "create publication todorok_outbox for table planner.outbox_event, activity.outbox_event;"
elif [[ "$publication_tables" != "$expected_tables" ]]; then
  echo "todorok_outbox publication table mismatch: $publication_tables" >&2
  exit 1
fi

slot_plugin="$(${psql_base[@]} --tuples-only --no-align --command "
select plugin from pg_replication_slots where slot_name = 'todorok_outbox_slot';
")"

if [[ -z "$slot_plugin" ]]; then
  "${psql_base[@]}" --set=ON_ERROR_STOP=1 --command \
    "select * from pg_create_logical_replication_slot('todorok_outbox_slot', 'pgoutput');"
elif [[ "$slot_plugin" != "pgoutput" ]]; then
  echo "todorok_outbox_slot plugin mismatch: $slot_plugin" >&2
  exit 1
fi
