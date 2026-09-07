#!/usr/bin/env bash
set -euo pipefail

export PGPASSWORD="$POSTGRES_PASSWORD"
psql_base=(psql --host postgres --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --no-psqlrc)

"${psql_base[@]}" --set=ON_ERROR_STOP=1 --set=database_name="$POSTGRES_DB" <<'EOSQL'
revoke create on database :"database_name" from debezium_app;
alter default privileges for role planner_app in schema planner
  revoke select on tables from debezium_app;
alter default privileges for role activity_app in schema activity
  revoke select on tables from debezium_app;
revoke all privileges on all tables in schema planner from debezium_app;
revoke all privileges on all tables in schema activity from debezium_app;
grant usage on schema planner, activity to debezium_app;
grant select on planner.outbox_event, activity.outbox_event to debezium_app;
EOSQL

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
