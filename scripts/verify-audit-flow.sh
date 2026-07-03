#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
BASE_URL="${BASE_URL:-http://localhost:8080}"
AUDIT_BASE_URL="${AUDIT_BASE_URL:-http://localhost:8088}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@mail.com}"
POLL_ATTEMPTS="${AUDIT_POLL_ATTEMPTS:-10}"
POLL_DELAY_SECONDS="${AUDIT_POLL_DELAY_SECONDS:-2}"

fail() {
  printf '[FAIL] %s\n' "$1" >&2
  exit 1
}

pass() {
  printf '[PASS] %s\n' "$1"
}

info() {
  printf '[INFO] %s\n' "$1"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required"
}

require_command curl
require_command grep
require_command sed
require_command tr

[ -f "$ENV_FILE" ] || fail ".env was not found. Copy .env.example to .env and configure the local stack."

set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
[ -n "$ADMIN_PASSWORD" ] || fail "ADMIN_PASSWORD must be set in .env"

WORK_DIR="$(mktemp -d)"
COOKIE_JAR="${WORK_DIR}/admin.cookies"
RESPONSE_FILE="${WORK_DIR}/response.json"
trap 'rm -rf "$WORK_DIR"' EXIT

request() {
  local method="$1"
  local url="$2"
  local expected_status="$3"
  local body="${4-}"
  local status
  local curl_args=(
    --silent --show-error
    --request "$method"
    --cookie "$COOKIE_JAR"
    --cookie-jar "$COOKIE_JAR"
    --header 'Accept: application/json'
    --output "$RESPONSE_FILE"
    --write-out '%{http_code}'
    --max-time 20
  )

  if [ -n "$body" ]; then
    curl_args+=(--header 'Content-Type: application/json' --data "$body")
  fi

  status="$(curl "${curl_args[@]}" "$url")" || fail "Request failed: ${method} ${url}"
  [ "$status" = "$expected_status" ] || {
    printf 'Response body: ' >&2
    tr '\n' ' ' < "$RESPONSE_FILE" >&2
    printf '\n' >&2
    fail "${method} ${url} returned HTTP ${status}; expected ${expected_status}"
  }
}

json_uuid() {
  local field="$1"
  tr -d '\n' < "$RESPONSE_FILE" |
    sed -nE "s/.*\"${field}\"[[:space:]]*:[[:space:]]*\"([0-9a-fA-F-]{36})\".*/\\1/p"
}

contains_event() {
  local event_type="$1"
  grep -Eq "\"eventType\"[[:space:]]*:[[:space:]]*\"${event_type}\"" "$RESPONSE_FILE"
}

poll_for_events() {
  local label="$1"
  local url="$2"
  shift 2
  local expected_events=("$@")
  local attempt
  local event_type
  local missing

  for attempt in $(seq 1 "$POLL_ATTEMPTS"); do
    request GET "$url" 200
    missing=""
    for event_type in "${expected_events[@]}"; do
      if ! contains_event "$event_type"; then
        missing="${missing} ${event_type}"
      fi
    done

    if [ -z "$missing" ]; then
      pass "${label}: ${expected_events[*]}"
      return 0
    fi

    info "${label} not complete on attempt ${attempt}/${POLL_ATTEMPTS}; missing:${missing}"
    sleep "$POLL_DELAY_SECONDS"
  done

  fail "${label} did not contain:${missing}"
}

info "Checking Audit API security"
status="$(curl --silent --show-error --output "$RESPONSE_FILE" --write-out '%{http_code}' --max-time 20 "${AUDIT_BASE_URL}/api/v1/audit")" ||
  fail "Audit Service is not reachable at ${AUDIT_BASE_URL}"
[ "$status" = "401" ] || fail "Unauthenticated Audit API returned HTTP ${status}; expected 401"
pass "Unauthenticated Audit API returns 401"

RUN_STARTED_AT="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

info "Logging in as ${ADMIN_EMAIL}"
request POST "${BASE_URL}/api/users/login" 200 \
  "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}\"}"
ADMIN_USER_ID="$(json_uuid userId)"
[ -n "$ADMIN_USER_ID" ] || fail "Admin login response did not contain userId"
pass "Admin login succeeded for user ${ADMIN_USER_ID}"

info "Triggering USER_LOGIN_FAILED"
status="$(curl --silent --show-error \
  --request POST \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --data "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}-audit-invalid\"}" \
  --output "$RESPONSE_FILE" \
  --write-out '%{http_code}' \
  --max-time 20 \
  "${BASE_URL}/api/users/login")" || fail "Failed-login request could not reach User Service"
[ "$status" = "401" ] || fail "Invalid login returned HTTP ${status}; expected 401"
pass "Failed login was rejected with 401"

run_id="$(date +%s)"
info "Creating task for Audit E2E run ${run_id}"
request POST "${BASE_URL}/api/tasks" 201 \
  "{\"title\":\"Audit E2E ${run_id}\",\"description\":\"Created by verify-audit-flow.sh\",\"priority\":\"MEDIUM\"}"
TASK_ID="$(json_uuid taskId)"
[ -n "$TASK_ID" ] || fail "Task creation response did not contain taskId"
pass "Created task ${TASK_ID}"

request PATCH "${BASE_URL}/api/tasks/${TASK_ID}" 200 \
  "{\"title\":\"Audit E2E updated ${run_id}\",\"description\":\"Updated by verify-audit-flow.sh\",\"priority\":\"HIGH\"}"
pass "Updated task"

request PATCH "${BASE_URL}/api/tasks/${TASK_ID}/assignee" 200 \
  "{\"assigneeUserId\":\"${ADMIN_USER_ID}\"}"
pass "Assigned task to admin"

request PATCH "${BASE_URL}/api/tasks/${TASK_ID}/status" 200 \
  '{"status":"IN_PROGRESS"}'
pass "Changed task status"

request POST "${BASE_URL}/api/notifications" 201 \
  "{\"recipientUserId\":\"${ADMIN_USER_ID}\",\"type\":\"SYSTEM\",\"channel\":\"IN_APP\",\"subject\":\"Audit E2E\",\"body\":\"Notification created by verify-audit-flow.sh ${run_id}\"}"
NOTIFICATION_ID="$(json_uuid notificationId)"
[ -n "$NOTIFICATION_ID" ] || fail "Notification creation response did not contain notificationId"
pass "Created notification ${NOTIFICATION_ID}"

request DELETE "${BASE_URL}/api/tasks/${TASK_ID}" 200
pass "Soft deleted task"

info "Polling Audit API while outbox and Kafka processing complete"
poll_for_events \
  "Task Service audit records" \
  "${AUDIT_BASE_URL}/api/v1/audit?aggregateId=${TASK_ID}&page=0&size=100&sort=createdAt,asc" \
  TASK_CREATED TASK_UPDATED TASK_ASSIGNED TASK_STATUS_CHANGED TASK_DELETED

AUDIT_ID="$(json_uuid auditId)"
[ -n "$AUDIT_ID" ] || fail "Task audit response did not contain auditId"

poll_for_events \
  "User Service audit records" \
  "${AUDIT_BASE_URL}/api/v1/audit?sourceService=user-service&from=${RUN_STARTED_AT}&page=0&size=100&sort=createdAt,desc" \
  USER_LOGIN_SUCCESS USER_LOGIN_FAILED

poll_for_events \
  "Notification Service audit records" \
  "${AUDIT_BASE_URL}/api/v1/audit?sourceService=notification-service&from=${RUN_STARTED_AT}&page=0&size=100&sort=createdAt,desc" \
  NOTIFICATION_CREATED NOTIFICATION_SYSTEM_CREATED

request GET "${AUDIT_BASE_URL}/api/v1/audit/${AUDIT_ID}" 200
grep -Eq "\"auditId\"[[:space:]]*:[[:space:]]*\"${AUDIT_ID}\"" "$RESPONSE_FILE" ||
  fail "Audit detail response did not contain auditId ${AUDIT_ID}"
pass "Audit detail lookup succeeded for ${AUDIT_ID}"

info "Verified taskId=${TASK_ID}"
info "Verified notificationId=${NOTIFICATION_ID}"
printf '\nAudit end-to-end verification completed successfully.\n'
