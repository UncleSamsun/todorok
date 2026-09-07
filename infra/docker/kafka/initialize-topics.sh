#!/usr/bin/env bash
set -euo pipefail

kafka_topics=/opt/kafka/bin/kafka-topics.sh
kafka_configs=/opt/kafka/bin/kafka-configs.sh
bootstrap_server=kafka:19092

create_topic() {
  local topic="$1"
  local partitions="$2"
  shift 2
  "$kafka_topics" --bootstrap-server "$bootstrap_server" \
    --create --if-not-exists --topic "$topic" \
    --partitions "$partitions" --replication-factor 1 "$@"
}

set_retention() {
  local topic="$1"
  local retention_ms="$2"
  "$kafka_configs" --bootstrap-server "$bootstrap_server" \
    --entity-type topics --entity-name "$topic" --alter \
    --add-config "retention.ms=$retention_ms,retention.bytes=1073741824"
}

create_topic todorok.task.v1 3
create_topic todorok.activity.v1 3
create_topic todorok.dead-letter 3
set_retention todorok.task.v1 604800000
set_retention todorok.activity.v1 604800000
set_retention todorok.dead-letter 2592000000

for topic in todorok.connect.configs todorok.connect.offsets todorok.connect.status; do
  create_topic "$topic" 1 --config cleanup.policy=compact
done
