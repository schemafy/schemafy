CREATE TABLE IF NOT EXISTS share_links (
    id               CHAR(26)      NOT NULL,
    project_id       CHAR(26)      NOT NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP     NULL,
    CONSTRAINT pk_share_links PRIMARY KEY (id),
    CONSTRAINT uk_share_links_project_id UNIQUE (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
