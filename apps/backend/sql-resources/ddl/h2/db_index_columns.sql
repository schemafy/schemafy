CREATE TABLE IF NOT EXISTS db_index_columns (
    id         CHAR(26)     NOT NULL,
    index_id   CHAR(26)     NOT NULL,
    column_id  CHAR(26)     NOT NULL,
    seq_no     INT          NOT NULL,
    sort_dir   VARCHAR(8)   NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version    BIGINT       NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP    NULL,
    CONSTRAINT pk_db_index_columns PRIMARY KEY (id)
);

ALTER TABLE db_index_columns
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uq_db_index_columns_index_column
    ON db_index_columns (index_id, column_id);
