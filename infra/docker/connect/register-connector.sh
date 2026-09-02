#!/usr/bin/env bash
set -euo pipefail

connect_url="${CONNECT_URL:-http://connect:8083}"
connector_name=todorok-postgres-outbox
rendered_request="$(mktemp)"
current_config="$(mktemp)"
trap 'rm -f "$rendered_request" "$current_config"' EXIT

envsubst '${POSTGRES_DB} ${DEBEZIUM_DB_PASSWORD}' \
  < /connect/connector-template.json > "$rendered_request"

http_code="$(curl --silent --show-error \
  --output "$current_config" --write-out '%{http_code}' \
  "$connect_url/connectors/$connector_name/config")"

if [[ "$http_code" == "404" ]]; then
  curl --fail --silent --show-error \
    --header 'Content-Type: application/json' \
    --data-binary "@$rendered_request" \
    "$connect_url/connectors" >/dev/null
  exit 0
fi

if [[ "$http_code" != "200" ]]; then
  cat "$current_config" >&2
  exit 1
fi

while IFS= read -r key; do
  [[ "$key" == "database.password" ]] && continue
  expected="$(jq -r --arg key "$key" '.config[$key]' "$rendered_request")"
  actual="$(jq -r --arg key "$key" '.[$key]' "$current_config")"
  if [[ "$expected" != "$actual" ]]; then
    echo "connector config mismatch for $key" >&2
    exit 1
  fi
done < <(jq -r '.config | keys[]' "$rendered_request")
