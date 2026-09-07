#!/usr/bin/env bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=planner_password="$PLANNER_DB_PASSWORD" \
  --set=activity_password="$ACTIVITY_DB_PASSWORD" \
  --set=notification_password="$NOTIFICATION_DB_PASSWORD" \
  --set=debezium_password="$DEBEZIUM_DB_PASSWORD" \
  --set=database_name="$POSTGRES_DB" <<'EOSQL'
create role planner_app login password :'planner_password';
create role activity_app login password :'activity_password';
create role notification_app login password :'notification_password';
create role debezium_app login replication password :'debezium_password';

create schema planner authorization planner_app;
create schema activity authorization activity_app;
create schema notification authorization notification_app;

revoke create on schema public from public;

grant connect, create on database :"database_name" to debezium_app;
grant usage on schema planner, activity to debezium_app;
alter default privileges for role planner_app in schema planner
    grant select on tables to debezium_app;
alter default privileges for role activity_app in schema activity
    grant select on tables to debezium_app;
EOSQL
