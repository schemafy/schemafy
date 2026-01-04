CREATE TABLE IF NOT EXISTS db_relationship_columns (
    id              CHAR(26)     NOT NULL,
    relationship_id CHAR(26)     NOT NULL,
    fk_column_id    CHAR(26)     NOT NULL,
    pk_column_id    CHAR(26)     NOT NULL,
    seq_no          INT          NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP    NULL,
    CONSTRAINT pk_db_relationship_columns PRIMARY KEY (id)
);
