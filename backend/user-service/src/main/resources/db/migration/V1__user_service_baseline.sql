CREATE SEQUENCE primary_key_seq START WITH 3 INCREMENT BY 1;

CREATE TABLE roles
(
    id           BIGINT                      NOT NULL,
    reference_id VARCHAR(255),
    created_by   BIGINT                      NOT NULL,
    updated_by   BIGINT                      NOT NULL,
    created_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    name         VARCHAR(255)                NOT NULL,
    authorities  TEXT,
    CONSTRAINT roles_pkey PRIMARY KEY (id)
);

CREATE TABLE users
(
    id                  BIGINT                      NOT NULL,
    reference_id        VARCHAR(255),
    created_by          BIGINT                      NOT NULL,
    updated_by          BIGINT                      NOT NULL,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at          TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    user_id             VARCHAR(255)                NOT NULL,
    first_name          VARCHAR(255),
    last_name           VARCHAR(255),
    email               VARCHAR(255)                NOT NULL,
    login_attempts      INTEGER,
    last_login          DATE,
    phone               VARCHAR(255),
    bio                 VARCHAR(255),
    image_url           VARCHAR(255),
    account_non_expired BOOLEAN                     NOT NULL DEFAULT TRUE,
    account_non_locked  BOOLEAN                     NOT NULL DEFAULT TRUE,
    enabled             BOOLEAN                     NOT NULL DEFAULT TRUE,
    mfa                 BOOLEAN                     NOT NULL DEFAULT FALSE,
    qr_code_secret      VARCHAR(255),
    qr_code_image_url   TEXT,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_user_id_unique UNIQUE (user_id),
    CONSTRAINT users_email_unique UNIQUE (email)
);

CREATE TABLE confirmations
(
    id           BIGINT                      NOT NULL,
    reference_id VARCHAR(255),
    created_by   BIGINT                      NOT NULL,
    updated_by   BIGINT                      NOT NULL,
    created_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    "key"        VARCHAR(255),
    user_id      BIGINT                      NOT NULL,
    CONSTRAINT confirmations_pkey PRIMARY KEY (id),
    CONSTRAINT confirmations_user_id_unique UNIQUE (user_id),
    CONSTRAINT confirmations_user_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE credentials
(
    id           BIGINT                      NOT NULL,
    reference_id VARCHAR(255),
    created_by   BIGINT                      NOT NULL,
    updated_by   BIGINT                      NOT NULL,
    created_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    password     VARCHAR(255),
    user_id      BIGINT                      NOT NULL,
    CONSTRAINT credentials_pkey PRIMARY KEY (id),
    CONSTRAINT credentials_user_id_unique UNIQUE (user_id),
    CONSTRAINT credentials_user_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE login_history
(
    id           BIGINT                      NOT NULL,
    reference_id VARCHAR(255),
    created_by   BIGINT                      NOT NULL,
    updated_by   BIGINT                      NOT NULL,
    created_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    user_id      BIGINT                      NOT NULL,
    login_time   TIMESTAMP(6) WITHOUT TIME ZONE,
    ip           VARCHAR(255),
    user_agent   VARCHAR(255),
    success      BOOLEAN                     NOT NULL,
    CONSTRAINT login_history_pkey PRIMARY KEY (id),
    CONSTRAINT login_history_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id),
    CONSTRAINT user_roles_user_fk FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT user_roles_role_fk FOREIGN KEY (role_id) REFERENCES roles (id)
);

INSERT INTO users
    (id, user_id, created_at, created_by, reference_id, updated_at, updated_by, email, first_name,
     last_name, login_attempts, phone, bio, image_url, mfa, enabled, account_non_expired,
     account_non_locked, qr_code_image_url, qr_code_secret, last_login)
VALUES
    (0, '123e4567-e89b-12d3-a456-426614174000', '2024-01-29 22:10:47.925942', 0, 'system',
     '2024-01-29 22:10:47.926642', 0, 'system@gmail.com', 'System', 'System', 0, '1234567890',
     'This is not a user but the system itself', 'https://cdn-icons-png.flaticon.com/128/2911/2911833.png',
     TRUE, TRUE, TRUE, TRUE, NULL, NULL, '2025-02-15');

INSERT INTO roles (id, name, authorities, created_at, created_by, reference_id, updated_at, updated_by)
VALUES
    (1, 'ADMIN',
     'user:create,user:read,user:update,user:delete,document:create,document:read,document:update,document:delete',
     '2024-01-29 22:10:47.925942', 0, 'system', '2024-01-29 22:10:47.926642', 0),
    (2, 'USER', 'document:create,document:read,document:update',
     '2024-01-29 22:10:47.925942', 0, 'system', '2024-01-29 22:10:47.926642', 0);
