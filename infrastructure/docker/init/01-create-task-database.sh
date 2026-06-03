#!/usr/bin/env bash
set -euo pipefail

TASK_DATABASE="${TASK_POSTGRES_DB:-tasks_db}"

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
SELECT 'CREATE DATABASE ${TASK_DATABASE}'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = '${TASK_DATABASE}'
)\gexec
EOSQL
