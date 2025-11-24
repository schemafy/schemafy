CREATE TABLE IF NOT EXISTS db_index_columns (
    id         CHAR(26)     NOT NULL,
    index_id   CHAR(26)     NOT NULL,
    column_id  CHAR(26)     NOT NULL,
    seq_no     INT          NOT NULL,
    sort_dir   VARCHAR(8)   NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP    NULL,
    CONSTRAINT pk_db_index_columns PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

