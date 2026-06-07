#!/usr/bin/env bash
set -euo pipefail

TASK_DATABASE="${TASK_POSTGRES_DB:-tasks_db}"

 psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=task_database="$TASK_DATABASE" <<-'EOSQL'
 SELECT format('CREATE DATABASE %I', :'task_database')
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = :'task_database'
)\gexec
EOSQL
