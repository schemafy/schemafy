CREATE TABLE IF NOT EXISTS db_relationship_columns (
    id              CHAR(26)     NOT NULL,
    relationship_id CHAR(26)     NOT NULL,
    src_column_id   CHAR(26)     NOT NULL,
    tgt_column_id   CHAR(26)     NOT NULL,
    seq_no          INT          NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP    NULL,
    CONSTRAINT pk_db_relationship_columns PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

