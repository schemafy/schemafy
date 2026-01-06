CREATE TABLE IF NOT EXISTS share_links (
    id               CHAR(26)      NOT NULL,
    project_id       CHAR(26)      NOT NULL,
    token_hash       VARBINARY(32) NOT NULL,
    role             VARCHAR(20)   NOT NULL,
    expires_at       TIMESTAMP     NULL,
    is_revoked       BOOLEAN       NOT NULL DEFAULT FALSE,
    last_accessed_at TIMESTAMP     NULL,
    access_count     BIGINT        NOT NULL DEFAULT 0,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP     NULL,
    CONSTRAINT pk_share_links PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS share_link_access_logs (
    id            CHAR(26)     NOT NULL,
    share_link_id CHAR(26)     NOT NULL,
    user_id       CHAR(26)     NULL,
    ip_address    VARCHAR(45)  NOT NULL,
    user_agent    VARCHAR(500) NULL,
    accessed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_share_link_access_logs PRIMARY KEY (id)
    );
