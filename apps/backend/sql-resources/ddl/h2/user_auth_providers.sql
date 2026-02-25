CREATE TABLE IF NOT EXISTS user_auth_providers (
    id               CHAR(26)     NOT NULL,
    user_id          CHAR(26)     NOT NULL,
    provider         VARCHAR(32)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP    NULL,
    CONSTRAINT pk_user_auth_providers PRIMARY KEY (id),
    CONSTRAINT uq_provider_user UNIQUE (provider, provider_user_id)
);
