CREATE TABLE IF NOT EXISTS share_links (
    id               CHAR(26)      NOT NULL,
    project_id       CHAR(26)      NOT NULL,
    code             VARCHAR(44)   NOT NULL,
    expires_at       TIMESTAMP     NULL,
    is_revoked       BOOLEAN       NOT NULL DEFAULT FALSE,
    last_accessed_at TIMESTAMP     NULL,
    access_count     BIGINT        NOT NULL DEFAULT 0,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP     NULL,
    CONSTRAINT pk_share_links PRIMARY KEY (id),
    CONSTRAINT uk_share_links_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
