CREATE TABLE IF NOT EXISTS db_constraints (
    id           CHAR(26)     NOT NULL,
    table_id     CHAR(26)     NOT NULL,
    name         VARCHAR(255) NOT NULL,
    kind         VARCHAR(32)  NOT NULL,
    check_expr   TEXT         NULL,
    default_expr TEXT         NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version      BIGINT       NOT NULL DEFAULT 0,
    deleted_at   TIMESTAMP    NULL,
    CONSTRAINT pk_db_constraints PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

