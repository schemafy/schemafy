CREATE TABLE IF NOT EXISTS db_relationship_columns (
    id              CHAR(26)     NOT NULL,
    relationship_id CHAR(26)     NOT NULL,
    fk_column_id    CHAR(26)     NOT NULL,
    pk_column_id    CHAR(26)     NOT NULL,
    seq_no          INT          NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP    NULL,
    CONSTRAINT pk_db_relationship_columns PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE db_relationship_columns
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uq_db_relationship_columns_rel_fk_column
    ON db_relationship_columns (relationship_id, fk_column_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_db_relationship_columns_rel_pk_column
    ON db_relationship_columns (relationship_id, pk_column_id);
