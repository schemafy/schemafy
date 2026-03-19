CREATE TABLE IF NOT EXISTS memos (
    id          CHAR(26)  NOT NULL,
    schema_id   CHAR(26)  NOT NULL,
    author_id   CHAR(26)  NOT NULL,
    positions   JSON      NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP NULL,
    CONSTRAINT pk_memos PRIMARY KEY (id)
);

ALTER TABLE memos
    ALTER COLUMN positions JSON NOT NULL;
