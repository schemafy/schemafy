CREATE TABLE IF NOT EXISTS share_links (
    id               CHAR(26)      NOT NULL,
    project_id       CHAR(26)      NOT NULL,
    token_hash       VARBINARY(32) NOT NULL,
    role             VARCHAR(20)   NOT NULL DEFAULT 'viewer',
    expires_at       TIMESTAMP     NULL,
    is_revoked       BOOLEAN       NOT NULL DEFAULT FALSE,
    last_accessed_at TIMESTAMP     NULL,
    access_count     BIGINT        NOT NULL DEFAULT 0,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP     NULL,
    CONSTRAINT pk_share_links PRIMARY KEY (id),
    CONSTRAINT uk_share_links_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_share_links_role CHECK (role IN ('viewer','commenter','editor')),
    INDEX idx_share_links_project_active (project_id, deleted_at),
    INDEX idx_share_links_expires_at (expires_at),
    CONSTRAINT fk_share_links_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS share_link_access_logs (
    id            CHAR(26)     NOT NULL,
    share_link_id CHAR(26)     NOT NULL,
    user_id       CHAR(26)     NULL,
    ip_address    VARCHAR(45)  NOT NULL,
    user_agent    VARCHAR(500) NULL,
    accessed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_share_link_access_logs PRIMARY KEY (id),
    INDEX idx_access_logs_link_time (share_link_id, accessed_at DESC),
    INDEX idx_access_logs_user_id (user_id),
    CONSTRAINT fk_access_logs_share_link FOREIGN KEY (share_link_id) REFERENCES share_links(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
