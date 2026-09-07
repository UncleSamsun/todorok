#!/usr/bin/env bash
set -euo pipefail

connect_url="${CONNECT_URL:-http://connect:8083}"
connector_name=todorok-postgres-outbox
curl_retry=(
  --retry 5
  --retry-all-errors
  --retry-delay 1
  --connect-timeout 5
  --max-time 30
)
rendered_request="$(mktemp)"
current_config="$(mktemp)"
rendered_config="$(mktemp)"
trap 'rm -f "$rendered_request" "$current_config" "$rendered_config"' EXIT

: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${DEBEZIUM_DB_PASSWORD:?DEBEZIUM_DB_PASSWORD is required}"
connector_config_update="${CONNECTOR_CONFIG_UPDATE:-false}"

jq --arg database "$POSTGRES_DB" --arg password "$DEBEZIUM_DB_PASSWORD" \
  '.config["database.dbname"] = $database
   | .config["database.password"] = $password' \
  /connect/connector-template.json > "$rendered_request"
jq '.config' "$rendered_request" > "$rendered_config"

http_code="$(curl "${curl_retry[@]}" --silent --show-error \
  --output "$current_config" --write-out '%{http_code}' \
  "$connect_url/connectors/$connector_name/config")"

if [[ "$http_code" == "404" ]]; then
  curl "${curl_retry[@]}" --fail --silent --show-error \
    --header 'Content-Type: application/json' \
    --data-binary "@$rendered_request" \
    "$connect_url/connectors" >/dev/null
  exit 0
fi

if [[ "$http_code" != "200" ]]; then
  cat "$current_config" >&2
  exit 1
fi

config_mismatch=false
while IFS= read -r key; do
  [[ "$key" == "database.password" ]] && continue
  expected="$(jq -r --arg key "$key" '.config[$key]' "$rendered_request")"
  actual="$(jq -r --arg key "$key" '.[$key]' "$current_config")"
  if [[ "$expected" != "$actual" ]]; then
    echo "connector config mismatch for $key" >&2
    config_mismatch=true
  fi
done < <(jq -r '.config | keys[]' "$rendered_request")

if [[ "$config_mismatch" == "true" && "$connector_config_update" != "true" ]]; then
  echo "set CONNECTOR_CONFIG_UPDATE=true to apply the reviewed config" >&2
  exit 1
fi

if [[ "$connector_config_update" == "true" ]]; then
  curl "${curl_retry[@]}" --fail --silent --show-error \
    --request PUT \
    --header 'Content-Type: application/json' \
    --data-binary "@$rendered_config" \
    "$connect_url/connectors/$connector_name/config" >/dev/null
fi
