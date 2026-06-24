#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${REPO_ROOT}/compose.yml"
ENV_FILE="${REPO_ROOT}/.env"

fail() {
  printf '[FAIL] %s\n' "$1" >&2
  exit 1
}

pass() {
  printf '[PASS] %s\n' "$1"
}

warn() {
  printf '[WARN] %s\n' "$1"
}

info() {
  printf '[INFO] %s\n' "$1"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required"
}

env_value() {
  local name="$1"
  printf '%s' "${!name-}"
}

expect_env() {
  local name="$1"
  local expected="$2"
  local actual

  actual="$(env_value "$name")"
  if [ "$actual" != "$expected" ]; then
    fail "${name} must be '${expected}' for Kafka notification verification; actual value is '${actual:-<unset>}'."
  fi
  pass "${name}=${expected}"
}

psql_query() {
  local database="$1"
  local query="$2"

  docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" tsp_postgres \
    psql -U "$POSTGRES_USER" -d "$database" -tA -F '|' -c "$query"
}

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

require_command docker
require_command grep

[ -f "$COMPOSE_FILE" ] || fail "compose.yml was not found at ${COMPOSE_FILE}"
[ -f "$ENV_FILE" ] || fail ".env was not found. Copy .env.example to .env and configure Kafka E2E values."

set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

POSTGRES_USER="${POSTGRES_USER:-user}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-}"
TASK_POSTGRES_DB="${TASK_POSTGRES_DB:-tasks_db}"
NOTIFICATION_POSTGRES_DB="${NOTIFICATION_POSTGRES_DB:-notifications_db}"

[ -n "$POSTGRES_PASSWORD" ] || fail "POSTGRES_PASSWORD must be set in .env"

expect_env OUTBOX_PUBLISHER_ENABLED true
expect_env OUTBOX_PUBLISHER_ADAPTER kafka
expect_env NOTIFICATION_KAFKA_ENABLED true

assignment_rest_enabled="${NOTIFICATION_ASSIGNMENT_REST_ENABLED:-${NOTIFICATION_SERVICE_ASSIGNMENT_REST_ENABLED:-true}}"
if [ "$assignment_rest_enabled" != "false" ]; then
  fail "NOTIFICATION_ASSIGNMENT_REST_ENABLED or NOTIFICATION_SERVICE_ASSIGNMENT_REST_ENABLED must be false; actual value is '${assignment_rest_enabled}'."
fi
pass "REST assignment notifications are disabled by env"

task_topic="${OUTBOX_PUBLISHER_KAFKA_TOPIC:-${KAFKA_TASK_EVENTS_TOPIC:-platform.task-events}}"
notification_topic="${NOTIFICATION_KAFKA_TOPIC:-${KAFKA_TASK_EVENTS_TOPIC:-platform.task-events}}"
if [ "$task_topic" != "$notification_topic" ]; then
  fail "Task producer topic (${task_topic}) and notification consumer topic (${notification_topic}) must match."
fi
pass "Task producer and notification consumer use topic ${task_topic}"

docker info >/dev/null 2>&1 || fail "Cannot connect to Docker. Start Docker Desktop or the Docker service."

required_services=(
  kafka
  postgres
  config-server
  eureka-server
  task-service
  notification-service
  gateway
)

running_services="$(compose ps --services --status running)"
for service in "${required_services[@]}"; do
  if ! printf '%s\n' "$running_services" | grep -qx "$service"; then
    fail "Required service '${service}' is not running. Start with: docker compose --env-file .env -f compose.yml up -d --build"
  fi
done
pass "Required Docker Compose services are running"

docker exec tsp_postgres pg_isready -q >/dev/null 2>&1 ||
  fail "PostgreSQL is not accepting connections inside tsp_postgres"
pass "PostgreSQL is accepting connections"

docker exec tsp_kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list >/dev/null 2>&1 ||
  fail "Kafka broker is not reachable inside tsp_kafka"
pass "Kafka broker is reachable"

latest_event="$(
  psql_query "$TASK_POSTGRES_DB" "
    SELECT event_id, aggregate_id, payload::jsonb->>'newAssigneeUserId'
    FROM outbox_events
    WHERE event_type = 'TASK_ASSIGNED'
      AND status = 'PROCESSED'
    ORDER BY processed_at DESC NULLS LAST, created_at DESC
    LIMIT 1;
  "
)"

[ -n "$latest_event" ] || fail "No PROCESSED TASK_ASSIGNED outbox event found. Create or assign a task after enabling Kafka E2E mode."

IFS='|' read -r event_id task_id assignee_user_id <<< "$latest_event"
[ -n "$event_id" ] || fail "Latest processed event did not include event_id"
[ -n "$task_id" ] || fail "Latest processed event did not include aggregate task_id"
[ -n "$assignee_user_id" ] || fail "Latest processed TASK_ASSIGNED event did not include newAssigneeUserId"
pass "Found processed TASK_ASSIGNED outbox event ${event_id} for task ${task_id}"

consumed_count="$(
  psql_query "$NOTIFICATION_POSTGRES_DB" "
    SELECT count(*)
    FROM event_consumption_log
    WHERE event_id = '${event_id}'::uuid;
  "
)"

[ "$consumed_count" = "1" ] ||
  fail "Expected exactly one event_consumption_log row for event ${event_id}; found ${consumed_count}."
pass "event_consumption_log contains exactly one row for ${event_id}"

notification_count="$(
  psql_query "$NOTIFICATION_POSTGRES_DB" "
    SELECT count(*)
    FROM notifications
    WHERE source_service = 'task-service'
      AND source_entity_type = 'TASK'
      AND source_entity_id = '${task_id}'::uuid
      AND recipient_user_id = '${assignee_user_id}'::uuid
      AND type = 'TASK_ASSIGNED';
  "
)"

[ "$notification_count" = "1" ] ||
  fail "Expected exactly one TASK_ASSIGNED notification for task ${task_id} and recipient ${assignee_user_id}; found ${notification_count}."
pass "notifications contains exactly one Kafka-created TASK_ASSIGNED notification"

if compose logs --no-color task-service 2>/dev/null |
  grep -q "Skipping synchronous REST assignment notification because assignment REST notifications are disabled"; then
  pass "task-service logs show the synchronous REST assignment notification path was skipped"
else
  warn "Could not find the REST-assignment skip log in task-service logs. If logs were rotated or the test used the assignment endpoint, verify NOTIFICATION_ASSIGNMENT_REST_ENABLED=false and repeat with task creation that includes assigneeUserId."
fi

info "Latest verified eventId=${event_id}"
info "Latest verified taskId=${task_id}"
info "Latest verified recipientUserId=${assignee_user_id}"
printf '\nKafka notification flow verification completed.\n'
