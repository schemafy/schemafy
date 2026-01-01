CREATE TABLE IF NOT EXISTS db_columns (
    id                CHAR(26)     NOT NULL,
    table_id          CHAR(26)     NOT NULL,
    name              VARCHAR(255) NOT NULL,
    seq_no  INT          NOT NULL,
    data_type         VARCHAR(64)  NOT NULL,
    length_scale      VARCHAR(64)  NULL,
    is_auto_increment BOOLEAN      NULL,
    charset           VARCHAR(64)  NULL,
    collation         VARCHAR(64)  NULL,
    comment           TEXT         NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP    NULL,
    CONSTRAINT pk_db_columns PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

