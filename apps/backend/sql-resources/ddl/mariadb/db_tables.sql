CREATE TABLE IF NOT EXISTS db_tables (
    id          CHAR(26)     NOT NULL,
    schema_id   CHAR(26)     NOT NULL,
    name        VARCHAR(255) NOT NULL,
    charset     VARCHAR(64)  NULL,
    collation   VARCHAR(64)  NULL,
    extra       JSON         NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NULL,
    CONSTRAINT pk_db_tables PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

