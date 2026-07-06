CREATE TABLE IF NOT EXISTS mcp_tokens (
    id             CHAR(26)      NOT NULL,
    user_id        CHAR(26)      NOT NULL,
    scope          VARCHAR(255)  NOT NULL,
    issued_at      TIMESTAMP     NOT NULL,
    expires_at     TIMESTAMP     NOT NULL,
    revoked_at     TIMESTAMP     NULL,
    last_used_at   TIMESTAMP     NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP     NULL,
    CONSTRAINT pk_mcp_tokens PRIMARY KEY (id),
    INDEX idx_mcp_tokens_user (user_id, expires_at, revoked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
