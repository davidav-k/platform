#!/bin/sh
set -eu

NOTIFICATION_DATABASE="${NOTIFICATION_POSTGRES_DB:-notifications_db}"

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=notification_database="$NOTIFICATION_DATABASE" <<-'EOSQL'
SELECT format('CREATE DATABASE %I', :'notification_database')
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = :'notification_database'
)\gexec
EOSQL
