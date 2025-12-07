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
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP     NULL,
    CONSTRAINT pk_share_links PRIMARY KEY (id),
    CONSTRAINT uk_share_links_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_share_links_role CHECK (role IN ('viewer','commenter','editor'))
    );
CREATE INDEX IF NOT EXISTS idx_share_links_project_active ON share_links(project_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_share_links_expires_at ON share_links(expires_at);

CREATE TABLE IF NOT EXISTS share_link_access_logs (
    id            CHAR(26)     NOT NULL,
    share_link_id CHAR(26)     NOT NULL,
    user_id       CHAR(26)     NULL,
    ip_address    VARCHAR(45)  NOT NULL,
    user_agent    VARCHAR(500) NULL,
    accessed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_share_link_access_logs PRIMARY KEY (id)
    );
CREATE INDEX IF NOT EXISTS idx_access_logs_link_time ON share_link_access_logs(share_link_id, accessed_at);
CREATE INDEX IF NOT EXISTS idx_access_logs_user_id ON share_link_access_logs(user_id);
