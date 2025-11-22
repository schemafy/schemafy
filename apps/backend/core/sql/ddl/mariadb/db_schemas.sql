CREATE TABLE IF NOT EXISTS db_schemas (
    id              CHAR(26)     NOT NULL,
    project_id      CHAR(26)     NOT NULL,
    db_vendor_id    VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    charset         VARCHAR(64)  NULL,
    collation       VARCHAR(64)  NULL,
    vendor_option   TEXT         NULL,
    canvas_viewport TEXT         NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP    NULL,
    CONSTRAINT pk_db_schemas PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

