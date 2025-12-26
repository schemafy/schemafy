CREATE TABLE IF NOT EXISTS chat_messages (
    id           CHAR(26)     NOT NULL,
    project_id   CHAR(26)     NOT NULL,
    author_id    CHAR(26)     NOT NULL,
    body         TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP    NULL,
    CONSTRAINT pk_chat_messages PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_chat_messages_project_created ON chat_messages (project_id, created_at DESC);

