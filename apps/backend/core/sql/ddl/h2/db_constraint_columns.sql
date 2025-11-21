CREATE TABLE IF NOT EXISTS db_constraint_columns (
    id            CHAR(26)     NOT NULL,
    constraint_id CHAR(26)     NOT NULL,
    column_id     CHAR(26)     NOT NULL,
    seq_no        INT          NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP    NULL,
    CONSTRAINT pk_db_constraint_columns PRIMARY KEY (id)
);

