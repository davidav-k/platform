SELECT 'CREATE DATABASE tasks_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tasks_db')\gexec

SELECT 'CREATE DATABASE notifications_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notifications_db')\gexec
