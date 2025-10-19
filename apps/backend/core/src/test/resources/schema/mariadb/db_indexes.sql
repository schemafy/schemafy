CREATE TABLE IF NOT EXISTS db_indexes (
    id         CHAR(26)     NOT NULL,
    table_id   CHAR(26)     NOT NULL,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(32)  NOT NULL,
    comment    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP    NULL,
    CONSTRAINT pk_db_indexes PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

