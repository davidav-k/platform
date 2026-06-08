CREATE SEQUENCE task_primary_key_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE tasks
(
    id                 BIGINT                   NOT NULL DEFAULT nextval('task_primary_key_seq'),
    task_id            UUID                     NOT NULL,
    title              VARCHAR(200)             NOT NULL,
    description        TEXT,
    status             VARCHAR(32)              NOT NULL,
    priority           VARCHAR(32)              NOT NULL,
    assignee_user_id   UUID,
    created_by_user_id UUID                     NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at         TIMESTAMP WITH TIME ZONE,
    deleted_by_user_id UUID,
    version            BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT tasks_pkey PRIMARY KEY (id),
    CONSTRAINT tasks_task_id_unique UNIQUE (task_id),
    CONSTRAINT tasks_title_not_blank CHECK (char_length(btrim(title)) > 0),
    CONSTRAINT tasks_description_length CHECK (description IS NULL OR char_length(description) <= 5000),
    CONSTRAINT tasks_status_check CHECK (status IN ('NEW', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    CONSTRAINT tasks_priority_check CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT tasks_deleted_by_requires_deleted_at CHECK (
        (deleted_at IS NULL AND deleted_by_user_id IS NULL)
        OR
        (deleted_at IS NOT NULL AND deleted_by_user_id IS NOT NULL)
    )
);

ALTER SEQUENCE task_primary_key_seq OWNED BY tasks.id;

CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_priority ON tasks (priority);
CREATE INDEX idx_tasks_assignee_user_id ON tasks (assignee_user_id);
CREATE INDEX idx_tasks_created_by_user_id ON tasks (created_by_user_id);
CREATE INDEX idx_tasks_created_at ON tasks (created_at);
