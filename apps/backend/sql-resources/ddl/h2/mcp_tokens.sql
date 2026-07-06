CREATE TABLE IF NOT EXISTS mcp_tokens (
    id             CHAR(26)      NOT NULL,
    user_id        CHAR(26)      NOT NULL,
    scope          VARCHAR(255)  NOT NULL,
    issued_at      TIMESTAMP     NOT NULL,
    expires_at     TIMESTAMP     NOT NULL,
    revoked_at     TIMESTAMP     NULL,
    last_used_at   TIMESTAMP     NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP     NULL,
    CONSTRAINT pk_mcp_tokens PRIMARY KEY (id)
    );

CREATE INDEX IF NOT EXISTS idx_mcp_tokens_user ON mcp_tokens (user_id, expires_at, revoked_at);
