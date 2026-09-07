#!/usr/bin/env bash
set -euo pipefail

: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
: "${PLANNER_DB_PASSWORD:?PLANNER_DB_PASSWORD is required}"
: "${ACTIVITY_DB_PASSWORD:?ACTIVITY_DB_PASSWORD is required}"
: "${NOTIFICATION_DB_PASSWORD:?NOTIFICATION_DB_PASSWORD is required}"
: "${DEBEZIUM_DB_PASSWORD:?DEBEZIUM_DB_PASSWORD is required}"

export PGPASSWORD="$POSTGRES_PASSWORD"
psql_args=(
  psql -v ON_ERROR_STOP=1
  --username "$POSTGRES_USER"
  --dbname "$POSTGRES_DB"
  --no-psqlrc
)
if [[ -n "${POSTGRES_HOST:-}" ]]; then
  psql_args+=(--host "$POSTGRES_HOST")
fi

"${psql_args[@]}" \
  --set=planner_password="$PLANNER_DB_PASSWORD" \
  --set=activity_password="$ACTIVITY_DB_PASSWORD" \
  --set=notification_password="$NOTIFICATION_DB_PASSWORD" \
  --set=debezium_password="$DEBEZIUM_DB_PASSWORD" \
  --set=database_name="$POSTGRES_DB" \
  --set=credential_update="${DATABASE_CREDENTIAL_UPDATE:-false}" <<'EOSQL'
select format('create role planner_app login password %L', :'planner_password')
where not exists (select 1 from pg_roles where rolname = 'planner_app') \gexec
select format('create role activity_app login password %L', :'activity_password')
where not exists (select 1 from pg_roles where rolname = 'activity_app') \gexec
select format('create role notification_app login password %L', :'notification_password')
where not exists (select 1 from pg_roles where rolname = 'notification_app') \gexec
select format('create role debezium_app login replication password %L', :'debezium_password')
where not exists (select 1 from pg_roles where rolname = 'debezium_app') \gexec

select format('alter role planner_app password %L', :'planner_password')
where :'credential_update' = 'true' \gexec
select format('alter role activity_app password %L', :'activity_password')
where :'credential_update' = 'true' \gexec
select format('alter role notification_app password %L', :'notification_password')
where :'credential_update' = 'true' \gexec
select format('alter role debezium_app password %L', :'debezium_password')
where :'credential_update' = 'true' \gexec

select 'create schema planner authorization planner_app'
where not exists (select 1 from information_schema.schemata where schema_name = 'planner') \gexec
select 'create schema activity authorization activity_app'
where not exists (select 1 from information_schema.schemata where schema_name = 'activity') \gexec
select 'create schema notification authorization notification_app'
where not exists (select 1 from information_schema.schemata where schema_name = 'notification') \gexec

alter schema planner owner to planner_app;
alter schema activity owner to activity_app;
alter schema notification owner to notification_app;

revoke create on schema public from public;
grant connect on database :"database_name"
  to planner_app, activity_app, notification_app, debezium_app;
revoke create on database :"database_name"
  from planner_app, activity_app, notification_app, debezium_app;

grant usage on schema planner, activity to debezium_app;
alter default privileges for role planner_app in schema planner
  revoke select on tables from debezium_app;
alter default privileges for role activity_app in schema activity
  revoke select on tables from debezium_app;
revoke all privileges on all tables in schema planner from debezium_app;
revoke all privileges on all tables in schema activity from debezium_app;

do $$
begin
  if to_regclass('planner.outbox_event') is not null then
    grant select on planner.outbox_event to debezium_app;
  end if;
  if to_regclass('activity.outbox_event') is not null then
    grant select on activity.outbox_event to debezium_app;
  end if;
end
$$;
EOSQL
