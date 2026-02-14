CREATE TABLE IF NOT EXISTS db_constraint_columns (
    id            CHAR(26)     NOT NULL,
    constraint_id CHAR(26)     NOT NULL,
    column_id     CHAR(26)     NOT NULL,
    seq_no        INT          NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version       BIGINT       NOT NULL DEFAULT 0,
    deleted_at    TIMESTAMP    NULL,
    CONSTRAINT pk_db_constraint_columns PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE db_constraint_columns
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uq_db_constraint_columns_constraint_column
    ON db_constraint_columns (constraint_id, column_id);
