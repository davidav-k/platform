#!/usr/bin/env bash

set -euo pipefail

check_expected_status() {
  local name="$1"
  local url="$2"
  local expected_status="$3"
  local actual_status

  for _ in $(seq 1 30); do
    actual_status=$(curl --silent --output /dev/null --write-out "%{http_code}" --connect-timeout 2 --max-time 5 "$url") || actual_status="000"
    if [[ "$actual_status" == "$expected_status" ]]; then
      echo "[OK] $name returned HTTP $expected_status."
      return 0
    fi
    sleep 2
  done

  echo "[FAIL] $name returned HTTP $actual_status; expected $expected_status."
  return 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${REPO_ROOT}/compose.yml"
ENV_FILE="${REPO_ROOT}/.env"

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

pass() {
  printf 'OK: %s\n' "$1"
}

check_http() {
  local name="$1"
  local url="$2"

  if ! curl --fail --silent --show-error --max-time 10 "$url" >/dev/null; then
    fail "${name} is not reachable at ${url}. Check container logs."
  fi

  pass "${name} is reachable at ${url}"
}

command -v docker >/dev/null 2>&1 || fail "Docker CLI is not installed or is not on PATH."
docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 is not available. Install a Docker version that supports 'docker compose'."
docker info >/dev/null 2>&1 || fail "Cannot connect to the Docker daemon. Start Docker Desktop or the Docker service."
command -v curl >/dev/null 2>&1 || fail "curl is required for HTTP health checks."

[ -f "$COMPOSE_FILE" ] || fail "compose.yml was not found at ${COMPOSE_FILE}."
[ -f "$ENV_FILE" ] || fail ".env was not found. Copy .env.example to .env and set local values."

pass "Docker CLI, Docker Compose, Docker daemon, curl, compose.yml, and .env are available"

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

required_services=(
  postgres
  redis
  zipkin
  mailhog
  config-server
  eureka-server
  user-service
  task-service
  notification-service
  gateway
)

running_services="$(compose ps --services --status running)"

for service in "${required_services[@]}"; do
  if ! printf '%s\n' "$running_services" | grep -qx "$service"; then
    fail "Required container for '${service}' is not running. Run 'docker compose --env-file .env -f compose.yml up -d --build'."
  fi
done

pass "All required Docker Compose containers are running"

healthy_containers=(
  tsp_postgres
  tsp_redis
  tsp_config
  tsp_eureka
  tsp_user_service
  tsp_task_service
  tsp_notification_service
  tsp_gateway
)

healthy_services=(
  postgres
  redis
  config-server
  eureka-server
  user-service
  task-service
  notification-service
  gateway
)

for index in "${!healthy_containers[@]}"; do
  container="${healthy_containers[$index]}"
  service="${healthy_services[$index]}"
  status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$container" 2>/dev/null)" ||
    fail "Cannot inspect '${container}'. Recreate the local stack with Docker Compose."

  if [ "$status" != "healthy" ]; then
    fail "Container '${container}' health status is '${status}', expected 'healthy'. Check 'docker compose --env-file .env -f compose.yml logs ${service}'."
  fi
done

pass "All containers with Docker health checks are healthy"

docker exec tsp_postgres pg_isready -q >/dev/null 2>&1 ||
  fail "PostgreSQL is not accepting connections inside tsp_postgres."
pass "PostgreSQL is accepting connections"

[ "$(docker exec tsp_redis redis-cli ping 2>/dev/null)" = "PONG" ] ||
  fail "Redis did not respond with PONG inside tsp_redis."
pass "Redis responded with PONG"

check_http "Config Server" "http://localhost:8888/actuator/health"
check_http "Config Server repository" "http://localhost:8888/user-service/dev"
check_http "Eureka Server" "http://localhost:8761/actuator/health"
check_http "Eureka registry" "http://localhost:8761/eureka/apps"
check_http "User Service" "http://localhost:8085/actuator/health"
check_http "Task Service" "http://localhost:8086/actuator/health"
check_http "Notification Service" "http://localhost:8087/actuator/health"
check_http "API Gateway" "http://localhost:8080/actuator/health"
check_expected_status "Gateway notification route" "http://localhost:8080/api/notifications" "401"

gateway_route_status="$(
  curl --silent --show-error --output /dev/null --write-out '%{http_code}' --max-time 10 \
    "http://localhost:8080/api/users/verify/account?key=local-startup-route-check-not-a-uuid"
)" || fail "API Gateway could not route the public user-service verification request."

case "$gateway_route_status" in
  200|400)
    pass "API Gateway routed a read-only request to User Service (expected invalid-key response: HTTP ${gateway_route_status})"
    ;;
  *)
    fail "API Gateway user-service route returned HTTP ${gateway_route_status}; expected 200 or 400 from the read-only invalid-key probe."
    ;;
esac

printf '\nLocal platform stack verification passed.\n'
